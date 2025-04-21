package ma.fin.monitor.common.entity.monitor

import java.text.SimpleDateFormat
import java.util.Date
/**
 * 风险评分类 - 表示实体的风险评分
 */
case class EntityRiskScore(
  entity: String,                       // 实体名称
  score: Double,                        // 总体风险分数(0-1)
  frequencyScore: Double = 0.0,         // 频率因子评分
  sanctionScore: Double = 0.0,          // 制裁因子评分
  complaintScore: Double = 0.0,         // 投诉因子评分
  networkScore: Double = 0.0,           // 网络因子评分
  timestamp: Long,                      // 评分时间戳
  riskLevel: String = "",               // 风险等级(LOW, MEDIUM, HIGH) 
  riskFactors: Map[String, Double] = Map.empty // 详细风险因子
) {
  
  // 获取格式化的风险分数
  def formattedScore: String = {
    f"$score%.2f"
  }
  
  // 获取格式化的时间戳
  def formattedTimestamp: String = {
    val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    fmt.format(new Date(timestamp))
  }
  
  // 获取风险等级（如果未设置）
  def calculatedRiskLevel: String = {
    if (riskLevel.nonEmpty) riskLevel
    else if (score >= 0.7) "HIGH"
    else if (score >= 0.4) "MEDIUM"
    else "LOW"
  }
  
  // 获取最显著的风险因子
  def topRiskFactors(limit: Int = 3): List[(String, Double)] = {
    riskFactors.toList.sortBy(_._2)(Ordering.Double.reverse).take(limit)
  }
  
  override def toString = s"EntityRiskScore($entity: $formattedScore [$calculatedRiskLevel] " +
    s"at $formattedTimestamp)"
}

/**
 * 实体风险状态 - 用于跟踪实体状态
 */
class EntityProfile(
  val entityId: String,
  var firstSeen: Long = 0,
  var lastSeen: Long = 0,
  var mentionCount: Int = 0,
  var isSanctioned: Boolean = false,
  var complaintCount: Int = 0,
  var averageComplaintSeverity: Double = 0.0,
  var networkDegree: Int = 0,
  var riskFactors: Map[String, Double] = Map.empty
) {
  
  // 更新实体信息
  def update(timestamp: Long): Unit = {
    if (firstSeen == 0) firstSeen = timestamp
    lastSeen = timestamp
    mentionCount += 1
  }
  
  // 添加投诉
  def addComplaint(severity: Double): Unit = {
    val newTotal = (averageComplaintSeverity * complaintCount) + severity
    complaintCount += 1
    averageComplaintSeverity = newTotal / complaintCount
  }
  
  // 添加风险因子
  def addRiskFactor(factor: String, score: Double): Unit = {
    riskFactors += (factor -> score)
  }
  
  override def toString = s"EntityProfile($entityId: mentions=$mentionCount, " +
    s"complaints=$complaintCount, sanctioned=$isSanctioned)"
}