// integration/kafka/TransactionProducer.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <thread>
#include <chrono>
#include <librdkafka/rdkafkacpp.h>

// 简单的CSV解析函数
std::vector<std::string> parseCsvLine(const std::string& line) {
    std::vector<std::string> result;
    std::string field;
    bool inQuotes = false;
    
    for (char c : line) {
        if (c == '\"') {
            inQuotes = !inQuotes;
        } else if (c == ',' && !inQuotes) {
            result.push_back(field);
            field.clear();
        } else {
            field += c;
        }
    }
    
    // 添加最后一个字段
    result.push_back(field);
    
    return result;
}

// 回调类，用于监控消息传递状态
class DeliveryReportCb : public RdKafka::DeliveryReportCb {
public:
    void dr_cb(RdKafka::Message &message) {
        if (message.err()) {
            std::cerr << "Message delivery failed: " << message.errstr() << std::endl;
        } else {
            std::cout << "Message delivered to topic " << message.topic_name() 
                      << " [" << message.partition() << "] at offset " 
                      << message.offset() << std::endl;
        }
    }
};

int main(int argc, char** argv) {
    std::string brokers = "localhost:9092";
    std::string topic = "financial-transactions";
    std::string inputFile = "../data/transactions.csv";
    int messageDelay = 500; // 每条消息之间的延迟，单位是毫秒
    
    // 解析命令行参数
    for (int i = 1; i < argc; i += 2) {
        std::string arg = argv[i];
        if (arg == "--brokers" && i + 1 < argc) {
            brokers = argv[i + 1];
        } else if (arg == "--topic" && i + 1 < argc) {
            topic = argv[i + 1];
        } else if (arg == "--file" && i + 1 < argc) {
            inputFile = argv[i + 1];
        } else if (arg == "--delay" && i + 1 < argc) {
            messageDelay = std::stoi(argv[i + 1]);
        } else {
            std::cerr << "Usage: " << argv[0] << " [--brokers <brokers>] [--topic <topic>] "
                      << "[--file <path>] [--delay <ms>]" << std::endl;
            return 1;
        }
    }
    
    // 创建Kafka配置
    std::string errstr;
    RdKafka::Conf *conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    
    // 设置Broker列表
    if (conf->set("bootstrap.servers", brokers, errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << "Failed to set bootstrap.servers: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 设置投递报告回调
    DeliveryReportCb dr_cb;
    if (conf->set("dr_cb", &dr_cb, errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << "Failed to set dr_cb: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 创建生产者
    RdKafka::Producer *producer = RdKafka::Producer::create(conf, errstr);
    if (!producer) {
        std::cerr << "Failed to create producer: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 打开输入文件
    std::ifstream file(inputFile);
    if (!file.is_open()) {
        std::cerr << "Failed to open input file: " << inputFile << std::endl;
        delete producer;
        delete conf;
        return 1;
    }
    
    // 读取CSV头行
    std::string headerLine;
    std::getline(file, headerLine);
    
    // 逐行读取数据并发送到Kafka
    std::string line;
    int messageCount = 0;
    
    std::cout << "Starting to send messages to Kafka topic: " << topic << std::endl;
    
    while (std::getline(file, line)) {
        // 发送消息到Kafka
        RdKafka::ErrorCode err = producer->produce(
            topic,                               // Topic
            RdKafka::Topic::PARTITION_UA,        // 自动分区选择
            RdKafka::Producer::RK_MSG_COPY,      // 复制消息
            const_cast<char*>(line.c_str()),     // 消息内容
            line.size(),                         // 消息大小
            NULL, 0,                             // 键（可选）
            0,                                   // 消息时间戳（0表示使用当前时间）
            NULL                                 // 消息头部（可选）
        );
        
        if (err != RdKafka::ERR_NO_ERROR) {
            std::cerr << "Failed to produce message: " << RdKafka::err2str(err) << std::endl;
        }
        
        // 定期调用poll确保消息投递回调被执行
        producer->poll(0);
        
        // 增加消息计数
        messageCount++;
        
        // 每100条消息输出一次状态
        if (messageCount % 100 == 0) {
            std::cout << "Sent " << messageCount << " messages" << std::endl;
        }
        
        // 延迟模拟实时数据
        if (messageDelay > 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(messageDelay));
        }
    }
    
    // 等待所有未决消息的投递
    std::cout << "Flushing remaining messages..." << std::endl;
    producer->flush(10000);
    
    // 确认所有消息的状态
    if (producer->outq_len() > 0) {
        std::cerr << producer->outq_len() << " message(s) were not delivered" << std::endl;
    }
    
    // 关闭文件
    file.close();
    
    // 清理资源
    delete producer;
    delete conf;
    
    std::cout << "Successfully sent " << messageCount << " messages to Kafka" << std::endl;
    return 0;
}