package ma.fin.monitor.transaction2entity.transform

import ma.fin.monitor.common.entity.financial.EntityMention
import org.apache.flink.api.common.functions.AggregateFunction

/**
 * 投诉摘要聚合器 - 聚合投诉实体提及
 */
class ComplaintSummaryAggregator 
  extends AggregateFunction[EntityMention, ComplaintSummary, EntityMention] {

  // 创建累加器
  override def createAccumulator(): ComplaintSummary = ComplaintSummary()
  
  // 添加元素到累加器
  override def add(mention: EntityMention, accumulator: ComplaintSummary): ComplaintSummary = {
    // 更新计数
    accumulator.count += 1
    
    // 更新最新时间戳
    if (mention.timestamp > accumulator.latestTimestamp) {
      accumulator.latestTimestamp = mention.timestamp
      accumulator.latestContext = mention.context
    }
    
    // 保存实体信息
    accumulator.entity = mention.entity
    accumulator.entityType = mention.entityType
    accumulator.category = mention.category
    
    accumulator
  }
  
  // 获取结果
  override def getResult(accumulator: ComplaintSummary): EntityMention = {
    // 创建带有汇总信息的实体提及
    EntityMention(
      entity = accumulator.entity,
      entityType = accumulator.entityType,
      position = 0,
      context = s"Entity mentioned in ${accumulator.count} complaints. Latest: ${accumulator.latestContext}",
      timestamp = accumulator.latestTimestamp,
      sourceType = "COMPLAINT_SUMMARY",
      category = accumulator.category,
      confidence = 1.0,
      riskScore = calculateRiskScore(accumulator)
    )
  }
  
  // 合并累加器
  override def merge(a: ComplaintSummary, b: ComplaintSummary): ComplaintSummary = {
    val merged = ComplaintSummary()
    merged.entity = a.entity
    merged.entityType = a.entityType
    merged.category = a.category
    merged.count = a.count + b.count
    
    // 选择最新的时间戳
    if (a.latestTimestamp > b.latestTimestamp) {
      merged.latestTimestamp = a.latestTimestamp
      merged.latestContext = a.latestContext
    } else {
      merged.latestTimestamp = b.latestTimestamp
      merged.latestContext = b.latestContext
    }
    
    merged
  }
  
  // 计算风险分数 - 基于投诉数量的对数函数
  private def calculateRiskScore(summary: ComplaintSummary): Double = {
    // 投诉数量的对数函数，限制在0-1范围内
    Math.min(1.0, Math.log10(1 + summary.count) / 2.0)
  }
}

/**
 * 投诉摘要累加器
 */
case class ComplaintSummary(
  var entity: String = "",
  var entityType: String = "",
  var category: String = "",
  var count: Int = 0,
  var latestTimestamp: Long = 0,
  var latestContext: String = ""
)