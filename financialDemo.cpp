#include "RadixTree.h"
#include <iostream>
#include <iomanip>
#include <chrono>
#include <string>
#include <vector>
#include <tuple>

using namespace radix;

// Utility to measure execution time
template<typename Func>
double measureTime(Func&& func) {
    auto start = std::chrono::high_resolution_clock::now();
    func();
    auto end = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double, std::milli> duration = end - start;
    return duration.count();
}

// Convert entity type to string
std::string entityTypeToString(EntityType type) {
    switch (type) {
        case EntityType::GENERIC: return "Generic";
        case EntityType::COMPANY: return "Company";
        case EntityType::CURRENCY: return "Currency";
        case EntityType::METRIC: return "Financial Metric";
        default: return "Unknown";
    }
}

int main() {
    std::cout << "=== Financial Text Processing Demo ===" << std::endl;
    
    // Initialize RadixTree
    RadixTree tree;
    
    // Financial dictionary data - UK financial institutions and terms
    std::vector<std::tuple<std::string, EntityType, int>> dictionary = {
        // Companies - UK financial institutions
        {"barclays", EntityType::COMPANY, 100},
        {"barclays bank", EntityType::COMPANY, 95},
        {"hsbc", EntityType::COMPANY, 98},
        {"hsbc holdings", EntityType::COMPANY, 90},
        {"lloyds", EntityType::COMPANY, 85},
        {"lloyds banking group", EntityType::COMPANY, 80},
        {"natwest", EntityType::COMPANY, 75},
        {"natwest group", EntityType::COMPANY, 70},
        {"standard chartered", EntityType::COMPANY, 65},
        
        // Currencies
        {"pound", EntityType::CURRENCY, 100},
        {"british pound", EntityType::CURRENCY, 95},
        {"gbp", EntityType::CURRENCY, 90},
        {"euro", EntityType::CURRENCY, 85},
        {"eur", EntityType::CURRENCY, 80},
        {"dollar", EntityType::CURRENCY, 75},
        {"usd", EntityType::CURRENCY, 70},
        
        // Financial metrics
        {"profit", EntityType::METRIC, 100},
        {"profit margin", EntityType::METRIC, 95},
        {"revenue", EntityType::METRIC, 90},
        {"return on equity", EntityType::METRIC, 85},
        {"roe", EntityType::METRIC, 80},
        {"price to earnings", EntityType::METRIC, 75},
        {"pe ratio", EntityType::METRIC, 65},
        {"earnings per share", EntityType::METRIC, 60},
        {"eps", EntityType::METRIC, 55},
        {"dividend yield", EntityType::METRIC, 50}
    };
    
    // Load dictionary
    std::cout << "Loading financial dictionary..." << std::endl;
    double loadTime = measureTime([&]() {
        for (const auto& [word, type, freq] : dictionary) {
            tree.addWordWithType(word, type, freq);
        }
    });
    
    std::cout << "Loaded " << dictionary.size() << " terms in " 
              << std::fixed << std::setprecision(2) << loadTime << "ms" << std::endl;
    
    // Test serialization
    std::cout << "\nTesting serialization..." << std::endl;
    double saveTime = measureTime([&]() {
        tree.saveToFile("fin_dict.bin");
    });
    std::cout << "Dictionary saved in " << saveTime << "ms" << std::endl;
    
    // Create a new tree and load from file
    RadixTree loadedTree;
    double loadFileTime = measureTime([&]() {
        loadedTree.loadFromFile("fin_dict.bin");
    });
    std::cout << "Dictionary loaded in " << loadFileTime << "ms" << std::endl;
    
    // Show dictionary stats
    std::cout << "\nDictionary Statistics:" << std::endl;
    std::cout << "Words: " << tree.wordCount() << std::endl;
    std::cout << "Nodes: " << tree.nodeCount() << std::endl;
    std::cout << "Memory efficiency: " << std::fixed << std::setprecision(2) 
              << (static_cast<double>(tree.wordCount()) / static_cast<double>(tree.nodeCount())) * 100.0
              << "%" << std::endl;
    
    // Demo use cases
    std::cout << "\n=== Demo Use Cases ===" << std::endl;
    
    // 1. Company name autocompletion
    std::cout << "\nUse Case 1: Financial Institution Search" << std::endl;
    std::cout << "Query: 'bar'" << std::endl;
    std::vector<std::string> companySuggestions;
    int companyCount;
    double companySearchTime = measureTime([&]() {
        loadedTree.typedSearch("bar", EntityType::COMPANY, companySuggestions, companyCount);
    });
    
    std::cout << "Results (" << companySearchTime << "ms):" << std::endl;
    for (const auto& suggestion : companySuggestions) {
        std::cout << "  - " << suggestion << std::endl;
    }
    
    // 2. Currency search
    std::cout << "\nUse Case 2: Currency Search" << std::endl;
    std::cout << "Query: 'p'" << std::endl;
    
    std::vector<std::string> currencySuggestions;
    int currencyCount;
    double currencySearchTime = measureTime([&]() {
        loadedTree.typedSearch("p", EntityType::CURRENCY, currencySuggestions, currencyCount);
    });
    
    std::cout << "Results (" << currencySearchTime << "ms):" << std::endl;
    for (const auto& suggestion : currencySuggestions) {
        std::cout << "  - " << suggestion << std::endl;
    }
    
    // 3. Financial metrics search
    std::cout << "\nUse Case 3: Financial Metrics Search" << std::endl;
    std::cout << "Query: 'p'" << std::endl;
    
    std::vector<std::string> metricSuggestions;
    int metricCount;
    double metricSearchTime = measureTime([&]() {
        loadedTree.typedSearch("p", EntityType::METRIC, metricSuggestions, metricCount);
    });
    
    std::cout << "Results (" << metricSearchTime << "ms):" << std::endl;
    for (const auto& suggestion : metricSuggestions) {
        std::cout << "  - " << suggestion << std::endl;
    }
    
    // Interactive demo
    std::cout << "\n=== Interactive Demo ===" << std::endl;
    std::cout << "Enter a prefix to search (empty to exit):" << std::endl;
    
    std::string input;
    while (true) {
        std::cout << "\nPrefix: ";
        std::getline(std::cin, input);
        
        if (input.empty()) break;
        
        // Convert to lowercase
        std::transform(input.begin(), input.end(), input.begin(), 
            [](unsigned char c) { return std::tolower(c); });
        
        // Search all entity types
        for (int typeIdx = 0; typeIdx <= static_cast<int>(EntityType::METRIC); typeIdx++) {
            EntityType type = static_cast<EntityType>(typeIdx);
            
            std::vector<std::string> suggestions;
            int count;
            double searchTime = measureTime([&]() {
                loadedTree.typedSearch(input, type, suggestions, count);
            });
            
            if (count > 0) {
                std::cout << entityTypeToString(type) << " matches (" 
                          << searchTime << "ms):" << std::endl;
                          
                for (const auto& suggestion : suggestions) {
                    std::cout << "  - " << suggestion << std::endl;
                }
                std::cout << std::endl;
            }
        }
    }
    
    return 0;
}