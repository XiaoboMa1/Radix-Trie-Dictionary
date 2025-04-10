// src/TransactionConsumer.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <map>
#include <chrono>
#include <sstream>
#include <iomanip>
#include <algorithm> // 确保包含这个用于 std::transform
#include <librdkafka/rdkafkacpp.h> // 添加 librdkafka C++ API 头文件
#include "../include/radixTree.h"

// 将EntityType转换为字符串
std::string entityTypeToString(radix::EntityType type) {
    switch (type) {
        case radix::EntityType::GENERIC: return "Generic";
        case radix::EntityType::COMPANY: return "Company";
        case radix::EntityType::CURRENCY: return "Currency";
        case radix::EntityType::METRIC: return "Financial Metric";
        case radix::EntityType::COMPLIANCE: return "Compliance";
        case radix::EntityType::RISK: return "Risk";
        default: return "Unknown";
    }
}

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

// 转换为小写
std::string toLower(const std::string& str) {
    std::string result = str;
    std::transform(result.begin(), result.end(), result.begin(),
                   [](unsigned char c) { return std::tolower(c); });
    return result;
}

// 提取交易描述中的实体
void extractEntities(const std::string& description, const radix::RadixTree& tree, 
                   std::map<std::string, std::string>& foundEntities) {
    foundEntities.clear();
    
    // 转换为小写进行处理
    std::string lowerDesc = toLower(description);
    
    // 简单的单词分割
    std::istringstream iss(lowerDesc);
    std::vector<std::string> words;
    std::string word;
    
    while (iss >> word) {
        // 去除标点符号
        word.erase(std::remove_if(word.begin(), word.end(), 
                                 [](char c) { return std::ispunct(c) && c != '\''; }), 
                  word.end());
        
        if (!word.empty()) {
            words.push_back(word);
        }
    }
    
    // 检查单个单词
    for (const std::string& w : words) {
        radix::DictNode* node = tree.spell(w);
        if (node && node->terminal) {
            foundEntities[w] = entityTypeToString(node->entityType);
        }
    }
    
    // 检查多单词短语（最多3个词）
    for (size_t i = 0; i < words.size(); ++i) {
        // 检查双词短语
        if (i + 1 < words.size()) {
            std::string phrase = words[i] + " " + words[i + 1];
            radix::DictNode* node = tree.spell(phrase);
            if (node && node->terminal) {
                foundEntities[phrase] = entityTypeToString(node->entityType);
            }
            
            // 检查三词短语
            if (i + 2 < words.size()) {
                phrase += " " + words[i + 2];
                node = tree.spell(phrase);
                if (node && node->terminal) {
                    foundEntities[phrase] = entityTypeToString(node->entityType);
                }
            }
        }
    }
}


// 消费者回调类
class ConsumerCb : public RdKafka::ConsumeCb {
public:
    ConsumerCb(radix::RadixTree& tree) : tree_(tree), messageCount_(0) {}
    
    void consume_cb(RdKafka::Message &msg, void *opaque) override {
        switch (msg.err()) {
            case RdKafka::ERR__TIMED_OUT:
                // 超时，可以忽略
                break;
                
            case RdKafka::ERR_NO_ERROR:
                // 成功收到消息
                processMessage(msg);
                break;
                
            default:
                // 其他错误
                std::cerr << "Consumer error: " << msg.errstr() << std::endl;
                break;
        }
    }
    
    int getMessageCount() const {
        return messageCount_;
    }
    
private:
    radix::RadixTree& tree_;
    int messageCount_;
    
    void processMessage(RdKafka::Message &msg) {
        messageCount_++;
        
        // 提取消息内容
        std::string payload(static_cast<const char*>(msg.payload()), msg.len());
        
        // 解析CSV行
        std::vector<std::string> fields = parseCsvLine(payload);
        
        // 确保字段足够
        if (fields.size() < 7) {
            std::cerr << "Invalid message format, expected at least 7 fields, got " 
                      << fields.size() << std::endl;
            return;
        }
        
        // 提取重要字段
        std::string transactionId = fields[0];
        std::string timestamp = fields[1];
        std::string accountNumber = fields[2];
        std::string amount = fields[3];
        std::string currency = fields[4];
        std::string transactionType = fields[5];
        std::string description = fields[6];
        
        // 使用RadixTree分析交易描述
        std::map<std::string, std::string> foundEntities;
        extractEntities(description, tree_, foundEntities);
        
        // 输出分析结果
        std::cout << "Transaction: " << transactionId 
                  << " (" << timestamp << ")" << std::endl;
        std::cout << "  Amount: " << amount << " " << currency << std::endl;
        std::cout << "  Type: " << transactionType << std::endl;
        std::cout << "  Description: " << description << std::endl;
        
        if (!foundEntities.empty()) {
            std::cout << "  Detected Entities:" << std::endl;
            for (const auto& [entity, type] : foundEntities) {
                std::cout << "    - " << entity << " (" << type << ")" << std::endl;
            }
        } else {
            std::cout << "  No known entities detected." << std::endl;
        }
        
        std::cout << std::endl;
    }
};

int main(int argc, char** argv) {
    std::string brokers = "localhost:9092";
    std::string topic = "financial-transactions";
    std::string group = "transaction-analyzer";
    std::string dictionaryFile = "../data/financial_entities.csv";
    
    // 解析命令行参数
    for (int i = 1; i < argc; i += 2) {
        std::string arg = argv[i];
        if (arg == "--brokers" && i + 1 < argc) {
            brokers = argv[i + 1];
        } else if (arg == "--topic" && i + 1 < argc) {
            topic = argv[i + 1];
        } else if (arg == "--group" && i + 1 < argc) {
            group = argv[i + 1];
        } else if (arg == "--dictionary" && i + 1 < argc) {
            dictionaryFile = argv[i + 1];
        } else {
            std::cerr << "Usage: " << argv[0] << " [--brokers <brokers>] [--topic <topic>] "
                      << "[--group <group>] [--dictionary <path>]" << std::endl;
            return 1;
        }
    }
    
    // 加载字典
    radix::RadixTree tree;
    
    std::ifstream dictFile(dictionaryFile);
    if (!dictFile.is_open()) {
        std::cerr << "Failed to open dictionary file: " << dictionaryFile << std::endl;
        return 1;
    }
    
    // 跳过CSV头行
    std::string headerLine;
    std::getline(dictFile, headerLine);
    
    // 读取字典
    std::string line;
    int termCount = 0;
    
    while (std::getline(dictFile, line)) {
        std::vector<std::string> fields = parseCsvLine(line);
        if (fields.size() >= 3) {
            std::string term = fields[0];
            std::string typeStr = fields[1];
            int frequency = std::stoi(fields[2]);
            
            radix::EntityType type = radix::EntityType::GENERIC;
            
            if (typeStr == "COMPANY") {
                type = radix::EntityType::COMPANY;
            } else if (typeStr == "CURRENCY") {
                type = radix::EntityType::CURRENCY;
            } else if (typeStr == "METRIC") {
                type = radix::EntityType::METRIC;
            } else if (typeStr == "COMPLIANCE") {
                type = radix::EntityType::COMPLIANCE;
            } else if (typeStr == "RISK") {
                type = radix::EntityType::RISK;
            }
            
            tree.addWordWithType(term, type, frequency);
            termCount++;
        }
    }
    
    dictFile.close();
    std::cout << "Loaded " << termCount << " terms into the RadixTree dictionary." << std::endl;
    
    // 创建Kafka配置
    std::string errstr;
    RdKafka::Conf *conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    
    // 设置Broker列表
    if (conf->set("bootstrap.servers", brokers, errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << "Failed to set bootstrap.servers: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 设置消费者组ID
    if (conf->set("group.id", group, errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << "Failed to set group.id: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 设置自动提交（最大简化）
    if (conf->set("enable.auto.commit", "true", errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << "Failed to set enable.auto.commit: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 设置自动偏移重置（从最早的消息开始消费）
    if (conf->set("auto.offset.reset", "earliest", errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << "Failed to set auto.offset.reset: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 创建消费者回调
    ConsumerCb consumer_cb(tree);
    
    // 设置消费回调
    if (conf->set("consume_cb", &consumer_cb, errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << "Failed to set consume_cb: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 创建消费者
    RdKafka::Consumer *consumer = RdKafka::Consumer::create(conf, errstr);
    if (!consumer) {
        std::cerr << "Failed to create consumer: " << errstr << std::endl;
        delete conf;
        return 1;
    }
    
    // 创建Topic配置
    RdKafka::Conf *tconf = RdKafka::Conf::create(RdKafka::Conf::CONF_TOPIC);
    
    // 创建Topic
    RdKafka::Topic *ktopic = RdKafka::Topic::create(consumer, topic, tconf, errstr);
    if (!ktopic) {
        std::cerr << "Failed to create topic: " << errstr << std::endl;
        delete consumer;
        delete conf;
        delete tconf;
        return 1;
    }
    
    // 开始消费
    RdKafka::ErrorCode err = consumer->start(ktopic, 0, RdKafka::Topic::OFFSET_BEGINNING);
    if (err != RdKafka::ERR_NO_ERROR) {
        std::cerr << "Failed to start consumer: " << RdKafka::err2str(err) << std::endl;
        delete ktopic;
        delete consumer;
        delete conf;
        delete tconf;
        return 1;
    }
    
    std::cout << "Started consuming from topic: " << topic << std::endl;
    std::cout << "Press Ctrl+C to stop..." << std::endl;
    
    // 消费循环
    try {
        while (true) {
            consumer->consume_callback(ktopic, 0, 1000, &consumer_cb, nullptr);
            
            // 每1000条消息输出一次状态
            int messageCount = consumer_cb.getMessageCount();
            if (messageCount > 0 && messageCount % 1000 == 0) {
                std::cout << "Processed " << messageCount << " messages so far" << std::endl;
            }
        }
    } catch (const std::exception& e) {
        std::cerr << "Exception: " << e.what() << std::endl;
    } catch (...) {
        std::cerr << "Unknown exception" << std::endl;
    }
    
    // 停止消费
    consumer->stop(ktopic, 0);
    
    // 清理资源
    delete ktopic;
    delete consumer;
    delete conf;
    delete tconf;
    
    return 0;
}