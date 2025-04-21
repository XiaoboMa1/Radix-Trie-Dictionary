package ma.fin.monitor.common.entity.monitor

import java.text.SimpleDateFormat
import java.util.Date

/**
 * 异常警报类 - 表示检测到的异常
 */
case class AnomalyAlert(
  entity: String,            // 实体名称
  alertType: String,         // 警报类型
  severity: Int,             // 严重程度(1-5)
  zScore: Double,            // Z分数
  observedValue: Double,     // 观察值
  expectedValue: Double,     // 预期值
  timestamp: Long,           // 警报时间戳
  description: String = "",  // 警报描述
  evidence: String = ""      // 证据
) {
  
  // 获取格式化的时间戳
  def formattedTimestamp: String = {
    val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    fmt.format(new Date(timestamp))
  }
  
  // 获取偏差百分比
  def deviationPercentage: String = {
    if (expectedValue == 0) "∞%"
    else f"${((observedValue - expectedValue) / expectedValue) * 100}%.1f%%"
  }
  
  // 获取严重程度描述
  def severityText: String = severity match {
    case 1 => "VERY LOW"
    case 2 => "LOW"
    case 3 => "MEDIUM"
    case 4 => "HIGH"
    case 5 => "VERY HIGH"
    case _ => "UNKNOWN"
  }
  
  // 获取简短描述
  def shortDescription: String = {
    if (description.nonEmpty) description
    else s"Abnormal activity detected for $entity. Observed value ($observedValue) is " +
      s"${deviationPercentage} different from expected (${f"$expectedValue%.1f"})."
  }
  
  override def toString = s"AnomalyAlert($entity [$alertType]: $severityText at $formattedTimestamp)"
}

/**
 * 交易风险警报
 */
case class TransactionRiskAlert(
  transactionId: String,
  entityId: String,
  riskScore: Double,
  riskFactors: java.util.List[RiskFactor],
  timestamp: Long,
  description: String = ""
) {
  
  // 获取风险等级
  def riskLevel: String = {
    if (riskScore >= 0.7) "HIGH"
    else if (riskScore >= 0.4) "MEDIUM"
    else "LOW"
  }
  
  // 获取格式化的风险分数
  def formattedRiskScore: String = {
    f"$riskScore%.2f"
  }
  
  override def toString = s"TransactionRiskAlert($transactionId: $formattedRiskScore [$riskLevel])"
}

/**
 * 风险因子
 */
case class RiskFactor(
  factorType: String,
  score: Double
)