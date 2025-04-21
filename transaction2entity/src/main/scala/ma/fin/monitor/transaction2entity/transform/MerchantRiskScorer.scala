package ma.fin.monitor.transaction2entity.transform

import ma.fin.monitor.common.entity.financial.EntityMention
import ma.fin.monitor.common.entity.monitor.RiskScore
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation

/**
 * 商户风险评分计算器
 */
class MerchantRiskScorer 
  extends ProcessWindowFunction[EntityMention, EntityMention, String, TimeWindow] {
  
  // 状态变量，保存商户历史风险数据
  private var merchantState: ValueState[MerchantRiskProfile] = _
  
  override def open(parameters: Configuration): Unit = {
    val stateDesc = new ValueStateDescriptor[MerchantRiskProfile](
      "merchant-risk-profile", TypeInformation.of(classOf[MerchantRiskProfile]))
    merchantState = getRuntimeContext.getState(stateDesc)
  }
  
  override def process(
      merchant: String,
      context: Context,
      elements: Iterable[EntityMention],
      out: Collector[EntityMention]): Unit = {
    
    // 获取商户历史数据
    val profile = Option(merchantState.value()).getOrElse(MerchantRiskProfile())
    
    // 计算交易频率
    val currentCount = elements.size
    val averageCount = profile.calculateAverageFrequency()
    val frequencyFactor = if (averageCount > 0) 
      math.min(1.0, currentCount / (averageCount * 2.0)) 
    else 
      0.5
    
    // 检查是否存在制裁关联
    val sanctionMentions = elements.count(_.entityType == "SANCTIONED_ENTITY")
    val sanctionFactor = math.min(1.0, sanctionMentions * 0.2)
    
    // 计算最终风险分数
    val riskScore = (frequencyFactor * 0.6) + (sanctionFactor * 0.4)
    
    // 更新商户历史数据
    profile.addFrequency(currentCount)
    profile.sanctionAssociations += sanctionMentions
    merchantState.update(profile)
    
    // 输出带有风险分数的实体提及
    val outputMention = elements.head.copy()
    outputMention.riskScore = riskScore
    out.collect(outputMention)
  }
}

/**
 * 商户风险画像 - 保存在状态中
 */
case class MerchantRiskProfile(
  var transactionCounts: List[Int] = List(),
  var sanctionAssociations: List[Int] = List()
) {
  def addFrequency(count: Int): Unit = {
    transactionCounts = (count :: transactionCounts).take(30) // 保留最近30个窗口
  }
  
  def calculateAverageFrequency(): Double = {
    if (transactionCounts.isEmpty) 0.0
    else transactionCounts.sum.toDouble / transactionCounts.size
  }
}