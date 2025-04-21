package ma.fin.monitor.

import ma.fin.monitor.common.entity.financial.EntityMention
import ma.fin.monitor.common.entity.monitor.RiskScore
import ma.fin.monitor.common.utils.JedisWrapper
import ma.common.config.RedisConfig
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.util.Collector

import scala.collection.mutable

/**
 * 实体风险评分计算器
 */
class EntityRiskScorer(
    frequencyWeight: Double = 0.3,
    sanctionWeight: Double = 0.4,
    complaintWeight: Double = 0.2,
    networkWeight: Double = 0.1) 
  extends ProcessWindowFunction[EntityMention, RiskScore, String, TimeWindow] {
  
  // Redis配置
  private implicit val redisConfig = RedisConfig(
    Config.redisHost, Config.redisPort, Config.redisPassword, Config.redisDb)
  
  override def process(
      entity: String,
      context: Context,
      mentions: Iterable[EntityMention],
      out: Collector[RiskScore]): Unit = {
    
    val mentionsList = mentions.toList
    
    // 1. 计算频率分数 - 基于窗口内提及次数
    val frequencyScore = calculateFrequencyScore(mentionsList.size)
    
    // 2. 计算制裁分数 - 检查实体是否在制裁名单中
    val sanctionScore = checkSanctionStatus(entity)
    
    // 3. 计算投诉分数 - 基于投诉相关提及
    val complaintScore = calculateComplaintScore(mentionsList)
    
    // 4. 计算网络分数 - 基于高风险关联
    val networkScore = calculateNetworkScore(entity, mentionsList)
    
    // 5. 计算加权总分
    val totalRiskScore = (frequencyScore * frequencyWeight) +
                         (sanctionScore * sanctionWeight) +
                         (complaintScore * complaintWeight) +
                         (networkScore * networkWeight)
    
    // 收集风险因子
    val riskFactors = mutable.Map[String, Double]()
    if (frequencyScore > 0.3) riskFactors("FREQUENCY") = frequencyScore
    if (sanctionScore > 0) riskFactors("SANCTION") = sanctionScore
    if (complaintScore > 0.3) riskFactors("COMPLAINT") = complaintScore
    if (networkScore > 0.3) riskFactors("NETWORK") = networkScore
    
    // 输出风险评分
    out.collect(RiskScore(
      entity = entity,
      score = totalRiskScore,
      frequencyScore = frequencyScore,
      sanctionScore = sanctionScore,
      complaintScore = complaintScore,
      networkScore = networkScore,
      timestamp = context.window.getEnd,
      riskFactors = riskFactors.toMap
    ))
  }
  
  // 基于提及频率计算分数 (0-1)
  private def calculateFrequencyScore(mentionCount: Int): Double = {
    // 指数衰减函数，避免高频实体分数过高
    1.0 - math.exp(-0.05 * mentionCount)
  }
  
  // 检查是否在制裁名单中 (0 或 1)
  private def checkSanctionStatus(entity: String): Double = {
    var score = 0.0
    
    JedisWrapper.wrap(jedis => {
      // 检查实体名称是否在制裁名单中
      val isSanctioned = jedis.sismember("sanctioned:names", entity.toLowerCase())
      if (isSanctioned) {
        score = 1.0
      }
    }, s"Failed to check sanction status for entity: $entity")
    
    score
  }
  
  // 基于投诉计算分数 (0-1)
  private def calculateComplaintScore(mentions: List[EntityMention]): Double = {
    val complaintMentions = mentions.count(_.category == "COMPLAINT")
    val complaintRatio = complaintMentions.toDouble / math.max(1, mentions.size)
    
    // 分数受投诉比例和总量影响
    complaintRatio * (1.0 - math.exp(-0.1 * complaintMentions))
  }
  
  // 基于网络关联计算分数 (0-1)
  private def calculateNetworkScore(entity: String, mentions: List[EntityMention]): Double = {
    var score = 0.0
    
    // 检查高风险关联
    val highRiskAssociations = mentions.count(m => 
      m.context.toLowerCase.contains("fraud") || 
      m.context.toLowerCase.contains("sanction") ||
      m.context.toLowerCase.contains("money laundering")
    )
    
    // 基于高风险关联数量计算分数
    if (highRiskAssociations > 0) {
      score = 1.0 - math.exp(-0.2 * highRiskAssociations)
    }
    
    score
  }
}