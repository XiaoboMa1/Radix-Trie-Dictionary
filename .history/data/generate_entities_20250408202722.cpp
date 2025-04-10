// 创建data/financial_entities.csv文件
#include <fstream>
#include <iostream>
#include <vector>
#include <tuple>
#include <string>

void generateFinancialEntities() {
    std::ofstream file("data/financial_entities.csv");
    
    // 写入CSV头
    file << "term,type,frequency\n";
    
    // 英国金融机构
    std::vector<std::tuple<std::string, std::string, int>> banks = {
        {"barclays", "COMPANY", 100},
        {"barclays bank", "COMPANY", 95},
        {"barclays bank plc", "COMPANY", 90},
        {"hsbc", "COMPANY", 98},
        {"hsbc holdings", "COMPANY", 90},
        {"hsbc bank", "COMPANY", 85},
        {"hsbc bank plc", "COMPANY", 80},
        {"lloyds", "COMPANY", 85},
        {"lloyds banking group", "COMPANY", 80},
        {"lloyds bank", "COMPANY", 75},
        {"natwest", "COMPANY", 75},
        {"natwest group", "COMPANY", 70},
        {"natwest bank", "COMPANY", 65},
        {"standard chartered", "COMPANY", 65},
        {"standard chartered bank", "COMPANY", 60},
        {"santander", "COMPANY", 70},
        {"santander uk", "COMPANY", 65},
        {"royal bank of scotland", "COMPANY", 60},
        {"rbs", "COMPANY", 55},
        {"halifax", "COMPANY", 50},
        {"nationwide", "COMPANY", 65},
        {"nationwide building society", "COMPANY", 60},
        {"citi", "COMPANY", 50},
        {"citibank", "COMPANY", 45},
        {"citigroup", "COMPANY", 40},
        {"jp morgan", "COMPANY", 60},
        {"jpmorgan chase", "COMPANY", 55},
        {"morgan stanley", "COMPANY", 50},
        {"goldman sachs", "COMPANY", 55},
        {"bank of england", "COMPANY", 95},
        {"boe", "COMPANY", 90},
        {"financial conduct authority", "COMPANY", 85},
        {"fca", "COMPANY", 80},
        {"prudential regulation authority", "COMPANY", 75},
        {"pra", "COMPANY", 70},
        {"london stock exchange", "COMPANY", 65},
        {"lse", "COMPANY", 60}
    };
    
    // 货币类型
    std::vector<std::tuple<std::string, std::string, int>> currencies = {
        {"pound", "CURRENCY", 100},
        {"british pound", "CURRENCY", 95},
        {"gbp", "CURRENCY", 90},
        {"pound sterling", "CURRENCY", 85},
        {"euro", "CURRENCY", 85},
        {"eur", "CURRENCY", 80},
        {"dollar", "CURRENCY", 75},
        {"usd", "CURRENCY", 70},
        {"us dollar", "CURRENCY", 65},
        {"japanese yen", "CURRENCY", 50},
        {"jpy", "CURRENCY", 45},
        {"swiss franc", "CURRENCY", 40},
        {"chf", "CURRENCY", 35},
        {"canadian dollar", "CURRENCY", 35},
        {"cad", "CURRENCY", 30},
        {"australian dollar", "CURRENCY", 30},
        {"aud", "CURRENCY", 25},
        {"chinese yuan", "CURRENCY", 25},
        {"cny", "CURRENCY", 20},
        {"renminbi", "CURRENCY", 15},
        {"rmb", "CURRENCY", 10},
        {"hong kong dollar", "CURRENCY", 20},
        {"hkd", "CURRENCY", 15},
        {"singapore dollar", "CURRENCY", 15},
        {"sgd", "CURRENCY", 10}
    };
    
    // 金融指标
    std::vector<std::tuple<std::string, std::string, int>> metrics = {
        {"profit", "METRIC", 100},
        {"profit margin", "METRIC", 95},
        {"revenue", "METRIC", 90},
        {"return on equity", "METRIC", 85},
        {"roe", "METRIC", 80},
        {"return on assets", "METRIC", 75},
        {"roa", "METRIC", 70},
        {"price to earnings", "METRIC", 75},
        {"pe ratio", "METRIC", 65},
        {"price-to-earnings", "METRIC", 60},
        {"earnings per share", "METRIC", 60},
        {"eps", "METRIC", 55},
        {"dividend yield", "METRIC", 50},
        {"dividend", "METRIC", 45},
        {"book value", "METRIC", 40},
        {"net asset value", "METRIC", 35},
        {"nav", "METRIC", 30},
        {"ebitda", "METRIC", 80},
        {"ebit", "METRIC", 75},
        {"operating margin", "METRIC", 70},
        {"gross margin", "METRIC", 65},
        {"net margin", "METRIC", 60},
        {"debt to equity", "METRIC", 55},
        {"debt-to-equity", "METRIC", 50},
        {"current ratio", "METRIC", 45},
        {"quick ratio", "METRIC", 40},
        {"acid test", "METRIC", 35},
        {"free cash flow", "METRIC", 60},
        {"fcf", "METRIC", 55},
        {"market cap", "METRIC", 70},
        {"market capitalization", "METRIC", 65},
        {"beta", "METRIC", 50},
        {"alpha", "METRIC", 45},
        {"sharpe ratio", "METRIC", 40},
        {"volatility", "METRIC", 55},
        {"liquidity", "METRIC", 50},
        {"solvency", "METRIC", 45},
        {"capital adequacy ratio", "METRIC", 70},
        {"car", "METRIC", 65},
        {"tier 1 capital", "METRIC", 60},
        {"risk-weighted assets", "METRIC", 55},
        {"rwa", "METRIC", 50},
        {"leverage ratio", "METRIC", 60},
        {"cost of capital", "METRIC", 55},
        {"wacc", "METRIC", 50},
        {"weighted average cost of capital", "METRIC", 45},
        {"capm", "METRIC", 40},
        {"capital asset pricing model", "METRIC", 35},
        {"credit rating", "METRIC", 65},
        {"bond yield", "METRIC", 60},
        {"yield curve", "METRIC", 55},
        {"term structure", "METRIC", 50},
        {"swap rate", "METRIC", 45},
        {"libor", "METRIC", 75},
        {"sonia", "METRIC", 70}
    };
    
    // 合规与监管术语
    std::vector<std::tuple<std::string, std::string, int>> compliance = {
        {"aml", "COMPLIANCE", 100},
        {"anti-money laundering", "COMPLIANCE", 95},
        {"kyc", "COMPLIANCE", 90},
        {"know your customer", "COMPLIANCE", 85},
        {"cdd", "COMPLIANCE", 80},
        {"customer due diligence", "COMPLIANCE", 75},
        {"edd", "COMPLIANCE", 70},
        {"enhanced due diligence", "COMPLIANCE", 65},
        {"pep", "COMPLIANCE", 90},
        {"politically exposed person", "COMPLIANCE", 85},
        {"sanctions", "COMPLIANCE", 90},
        {"fatf", "COMPLIANCE", 85},
        {"financial action task force", "COMPLIANCE", 80},
        {"mifid", "COMPLIANCE", 75},
        {"markets in financial instruments directive", "COMPLIANCE", 70},
        {"mifid ii", "COMPLIANCE", 65},
        {"gdpr", "COMPLIANCE", 80},
        {"general data protection regulation", "COMPLIANCE", 75},
        {"cft", "COMPLIANCE", 70},
        {"countering the financing of terrorism", "COMPLIANCE", 65},
        {"sars", "COMPLIANCE", 60},
        {"suspicious activity reports", "COMPLIANCE", 55},
        {"csr", "COMPLIANCE", 50},
        {"corporate social responsibility", "COMPLIANCE", 45},
        {"esg", "COMPLIANCE", 80},
        {"environmental social governance", "COMPLIANCE", 75},
        {"brexit", "COMPLIANCE", 70},
        {"ftt", "COMPLIANCE", 65},
        {"financial transaction tax", "COMPLIANCE", 60}
    };
    
    // 风险术语
    std::vector<std::tuple<std::string, std::string, int>> risk = {
        {"market risk", "RISK", 100},
        {"credit risk", "RISK", 95},
        {"operational risk", "RISK", 90},
        {"liquidity risk", "RISK", 85},
        {"interest rate risk", "RISK", 80},
        {"currency risk", "RISK", 75},
        {"foreign exchange risk", "RISK", 70},
        {"fx risk", "RISK", 65},
        {"counterparty risk", "RISK", 80},
        {"sovereign risk", "RISK", 75},
        {"country risk", "RISK", 70},
        {"default risk", "RISK", 85},
        {"concentration risk", "RISK", 80},
        {"settlement risk", "RISK", 75},
        {"basis risk", "RISK", 70},
        {"systemic risk", "RISK", 85},
        {"systematic risk", "RISK", 80},
        {"idiosyncratic risk", "RISK", 75},
        {"specific risk", "RISK", 70},
        {"var", "RISK", 90},
        {"value at risk", "RISK", 85},
        {"expected shortfall", "RISK", 80},
        {"conditional var", "RISK", 75},
        {"stress test", "RISK", 85},
        {"scenario analysis", "RISK", 80},
        {"risk appetite", "RISK", 75},
        {"risk tolerance", "RISK", 70},
        {"risk management", "RISK", 90},
        {"fraud risk", "RISK", 85},
        {"cyber risk", "RISK", 80},
        {"conduct risk", "RISK", 75}
    };
    
    // 写入所有类别
    for (const auto& [term, type, freq] : banks) {
        file << term << "," << type << "," << freq << "\n";
    }
    
    for (const auto& [term, type, freq] : currencies) {
        file << term << "," << type << "," << freq << "\n";
    }
    
    for (const auto& [term, type, freq] : metrics) {
        file << term << "," << type << "," << freq << "\n";
    }
    
    for (const auto& [term, type, freq] : compliance) {
        file << term << "," << type << "," << freq << "\n";
    }
    
    for (const auto& [term, type, freq] : risk) {
        file << term << "," << type << "," << freq << "\n";
    }
    
    file.close();
    std::cout << "Financial entities data generated successfully." << std::endl;
}

int main() {
    generateFinancialEntities();
    return 0;
}