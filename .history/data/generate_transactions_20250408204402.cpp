// 创建data/generate_transactions.cpp文件
#include <fstream>
#include <iostream>
#include <random>
#include <string>
#include <vector>
#include <chrono>
#include <iomanip>
#include <sstream>

std::string generateTimestamp() {
    auto now = std::chrono::system_clock::now();
    auto time_t_now = std::chrono::system_clock::to_time_t(now);
    
    // 随机生成过去30天内的时间
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> time_offset(0, 30 * 24 * 60 * 60); // 30天的秒数
    
    time_t random_time = time_t_now - time_offset(gen);
    
    std::tm tm = *std::localtime(&random_time);
    std::ostringstream oss;
    oss << std::put_time(&tm, "%Y-%m-%d %H:%M:%S");
    return oss.str();
}

std::string generateTransactionID() {
    static int counter = 1000000;
    return "TX" + std::to_string(++counter);
}

std::string generateAccountNumber() {
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dist(10000000, 99999999);
    return std::to_string(dist(gen));
}

double generateAmount() {
    std::random_device rd;
    std::mt19937 gen(rd());
    
    // 大多数交易在10-1000英镑范围内
    std::exponential_distribution<> dist(0.005); // 均值为200
    double amount = dist(gen);
    
    // 截断到10000英镑以内，避免极端值
    amount = std::min(amount, 10000.0);
    amount = std::max(amount, 5.0);  // 最小5英镑
    
    // 保留两位小数
    return std::round(amount * 100) / 100;
}

std::string generateCurrency() {
    std::vector<std::string> currencies = {"GBP", "EUR", "USD", "JPY", "CHF", "CAD", "AUD"};
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dist(0, currencies.size() - 1);
    
    // 偏向GBP（英镑）
    if (dist(gen) < 3) {
        return "GBP";
    }
    
    return currencies[dist(gen)];
}

std::string generateTransactionType() {
    std::vector<std::string> types = {"PAYMENT", "TRANSFER", "WITHDRAWAL", "DEPOSIT", "FEE", "INTEREST", "EXCHANGE"};
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dist(0, types.size() - 1);
    return types[dist(gen)];
}

std::string generateDescription() {
    std::vector<std::string> payees = {
        "Amazon UK", "Tesco", "Sainsbury's", "Barclays Bank", "HSBC Bank", "Lloyds Bank", 
        "NatWest Bank", "Vodafone UK", "British Gas", "EDF Energy", "Thames Water", 
        "Transport for London", "Virgin Media", "BT Group", "Sky UK", "Netflix", 
        "Spotify", "Deliveroo", "Uber", "Just Eat", "Premier Inn", "British Airways", 
        "EasyJet", "Ryanair", "John Lewis", "Marks & Spencer", "ASOS", "Boots", 
        "Costa Coffee", "Starbucks", "Pret A Manger", "McDonald's", "KFC", "Nando's"
    };
    
    std::vector<std::string> prefixes = {
        "Payment to ", "Transfer to ", "Withdrawal for ", "Deposit from ", 
        "Fee for service from ", "Interest payment from ", "Exchange to ", 
        "Direct debit for ", "Standing order to ", "Online purchase at ", 
        "In-store purchase at ", "Subscription payment to ", "Bill payment to ",
        "Faster payment to ", "International transfer to "
    };
    
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> payee_dist(0, payees.size() - 1);
    std::uniform_int_distribution<> prefix_dist(0, prefixes.size() - 1);
    
    return prefixes[prefix_dist(gen)] + payees[payee_dist(gen)];
}

std::string generateStatus() {
    std::vector<std::string> statuses = {"COMPLETED", "PENDING", "FAILED", "CANCELLED", "REJECTED", "REFUNDED"};
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dist(0, statuses.size() - 1);
    
    // 大多数交易成功完成
    if (dist(gen) < 4) {
        return "COMPLETED";
    }
    
    return statuses[dist(gen)];
}

std::string generateChannel() {
    std::vector<std::string> channels = {"ONLINE", "MOBILE", "ATM", "BRANCH", "TELEPHONE", "DIRECT_DEBIT", "STANDING_ORDER"};
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dist(0, channels.size() - 1);
    return channels[dist(gen)];
}

int main() {
    const int NUM_TRANSACTIONS = 10000;  // 生成10000条交易记录
    
    std::ofstream file("transactions.csv");
    if (!file.is_open()) {
        std::cerr << "Failed to open file for writing!" << std::endl;
        return 1;
    }
    
    // 写入CSV头
    file << "transaction_id,timestamp,account_number,amount,currency,transaction_type,description,status,channel\n";
    
    // 生成交易数据
    for (int i = 0; i < NUM_TRANSACTIONS; ++i) {
        std::string transaction_id = generateTransactionID();
        std::string timestamp = generateTimestamp();
        std::string account_number = generateAccountNumber();
        double amount = generateAmount();
        std::string currency = generateCurrency();
        std::string transaction_type = generateTransactionType();
        std::string description = generateDescription();
        std::string status = generateStatus();
        std::string channel = generateChannel();
        
        // 写入CSV行
        file << transaction_id << ","
             << timestamp << ","
             << account_number << ","
             << std::fixed << std::setprecision(2) << amount << ","
             << currency << ","
             << transaction_type << ","
             << "\"" << description << "\"" << ","  // 加引号，避免描述中的逗号影响CSV格式
             << status << ","
             << channel << "\n";
    }
    
    file.close();
    std::cout << "Generated " << NUM_TRANSACTIONS << " transactions successfully." << std::endl;
    
    return 0;
}