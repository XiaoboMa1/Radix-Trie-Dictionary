package ma.fin.monitor.transaction2entity.transform

import ma.fin.monitor.common.entity.financial.ComplaintRecord
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

import java.util.regex.Pattern

/**
 * 投诉主题分类器
 */
class ComplaintTopicClassifier extends ProcessFunction[ComplaintRecord, ComplaintRecord] {
  
  // 主题模式
  private val topicPatterns = Map(
    "fees" -> Pattern.compile("(?i)(hidden|excessive|unexpected|undisclosed)\\s+fee"),
    "interest_rates" -> Pattern.compile("(?i)(high|excessive|undisclosed)\\s+interest\\s+rate"),
    "customer_service" -> Pattern.compile("(?i)(poor|terrible|inadequate)\\s+(service|support|response)"),
    "account_access" -> Pattern.compile("(?i)(unable|difficult|impossible)\\s+to\\s+access"),
    "fraud" -> Pattern.compile("(?i)(fraud|scam|unauthorized|suspicious)"),
    "data_privacy" -> Pattern.compile("(?i)(data\\s+breach|privacy\\s+violation|personal\\s+information)")
  )
  
  override def processElement(
      complaint: ComplaintRecord,
      ctx: ProcessFunction[ComplaintRecord, ComplaintRecord]#Context,
      out: Collector[ComplaintRecord]): Unit = {
    
    // 检测主题
    val detectedTopics = topicPatterns.flatMap { case (topic, pattern) =>
      val matcher = pattern.matcher(complaint.text)
      if (matcher.find()) Some(topic) else None
    }.toSet
    
    // 如果没有检测到主题，使用原始issue类型
    val finalTopics = if (detectedTopics.isEmpty) Set(complaint.issueType) else detectedTopics
    
    // 为每个主题创建副本
    finalTopics.foreach { topic =>
      val enhancedComplaint = complaint.copy()
      enhancedComplaint.detectedTopic = topic
      out.collect(enhancedComplaint)
    }
  }
}