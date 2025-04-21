package ma.fin.monitor.common.utils

import java.time.format.DateTimeFormatter

// 修改业务常量，从电商转为金融监控
object Constants {
  // 将电商主题替换为金融合规主题
  val FINANCIAL_COMPLAINTS_TOPIC = "financial-complaints"
  val FINANCIAL_TRANSACTIONS_TOPIC = "financial-transactions"
  val ENTITY_MENTIONS_TOPIC = "entity-mentions"
  val ENTITY_RISK_ALERTS_TOPIC = "entity-risk-alerts"
  
  // 制裁名单来源分类
  val SANCTIONS_UK_COMPANIES_HOUSE = "UK_COMPANIES_HOUSE"
  val SANCTIONS_UK_FCDO = "UK_FCDO"
  val SANCTIONS_UK_HMTOFSI = "UK_HMTOFSI"
  
  // 风险评分阈值
  val HIGH_RISK_THRESHOLD = 0.7
  val MEDIUM_RISK_THRESHOLD = 0.4
    // 实体类型
  val ENTITY_TYPE_FINANCIAL_INSTITUTION = "FINANCIAL_INSTITUTION"
  val ENTITY_TYPE_INDIVIDUAL = "INDIVIDUAL"
  val ENTITY_TYPE_SANCTIONED = "SANCTIONED_ENTITY"
  val ENTITY_TYPE_PEP = "POLITICALLY_EXPOSED_PERSON"
  
  // 警报类型
  val ALERT_TYPE_SANCTIONED_ENTITY = "SANCTIONED_ENTITY"
  val ALERT_TYPE_SUSPICIOUS_TRANSACTION = "SUSPICIOUS_TRANSACTION"
  val ALERT_TYPE_HIGH_RISK_ENTITY = "HIGH_RISK_ENTITY"
  val ALERT_TYPE_FREQUENCY_ANOMALY = "FREQUENCY_ANOMALY"

    // 消费者模式常量
  val CONSUMER_MODE_EARLIEST = "earliest"
  val CONSUMER_MODE_LATEST = "latest"
  val CONSUMER_MODE_TIMESTAMP = "timestamp"
  val CONSUMER_MODE_COMMITTED = "committed"
    // 日期时间格式化器
  val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  val DT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
  val DATE_TIME_MIN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  
}