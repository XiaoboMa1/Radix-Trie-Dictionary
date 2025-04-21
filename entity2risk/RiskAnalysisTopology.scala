package ma.fin.monitor.entity2risk

import ma.fin.monitor.common.sink.KafkaConfig
import ma.fin.monitor.common.entity.financial.EntityMention
import ma.fin.monitor.common.entity.monitor.{EntityFrequency, AnomalyAlert, RiskScore}
import ma.fin.monitor.common.sink.SinkFactory
import ma.fin.monitor.common.source.SourceFactory
import ma.fin.monitor.common.utils.Constants
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.assigners.{SlidingEventTimeWindows, TumblingEventTimeWindows}

/**
 * 风险分析拓扑
 */
object RiskAnalysisTopology {

  def build(kafkaConfig: KafkaConfig)(implicit env: StreamExecutionEnvironment): Unit = {
    // 读取实体提及流
    val entityMentionStream = SourceFactory.createKafkaStream[EntityMention](
      kafkaConfig, Constants.ENTITY_MENTIONS_TOPIC)
      .assignTimestampsAndWatermarks(
        WatermarkStrategy
          .forBoundedOutOfOrderness(Duration.ofMinutes(1))
          .withTimestampAssigner((mention, _) => mention.timestamp)
      )
    
    // 实体频率计算
    val entityFrequencyStream = entityMentionStream
      .keyBy(_.entity)
      .window(SlidingEventTimeWindows.of(Time.hours(4), Time.minutes(10)))
      .aggregate(new EntityFrequencyAggregator())
      .name("entity-frequency-calculation")
    
    // 频率异常检测
    val frequencyAnomalyStream = entityFrequencyStream
      .keyBy(_.entity)
      .process(new FrequencyAnomalyDetector(zScoreThreshold = 2.5))
      .name("frequency-anomaly-detection")
    
    // 实体风险评分计算
    val entityRiskStream = entityMentionStream
      .keyBy(_.entity)
      .window(TumblingEventTimeWindows.of(Time.hours(24)))
      .process(new EntityRiskScorer(
        frequencyWeight = 0.3,
        sanctionWeight = 0.4,
        complaintWeight = 0.2,
        networkWeight = 0.1
      ))
      .name("entity-risk-scoring")
    
    // 输出高风险告警
    val highRiskAlertStream = entityRiskStream
      .filter(_.score > Constants.HIGH_RISK_THRESHOLD)
      .map(score => AnomalyAlert(
        entity = score.entity,
        alertType = "HIGH_RISK_ENTITY",
        severity = calculateSeverity(score.score),
        description = s"High risk entity detected: ${score.entity} with score ${score.score}",
        timestamp = score.timestamp
      ))
      .name("high-risk-alerts")
    
    // 输出到Kafka
    highRiskAlertStream
      .addSink(SinkFactory.createKafkaProducer[AnomalyAlert](Constants.ENTITY_RISK_ALERTS_TOPIC))
      .name("risk-alert-sink")
    
    // 频率异常也输出到告警
    frequencyAnomalyStream
      .addSink(SinkFactory.createKafkaProducer[AnomalyAlert](Constants.ENTITY_RISK_ALERTS_TOPIC))
      .name("anomaly-alert-sink")
  }
  
  // 根据风险分数计算严重程度(1-5)
  private def calculateSeverity(score: Double): Int = {
    if (score > 0.9) 5
    else if (score > 0.8) 4
    else if (score > 0.7) 3
    else if (score > 0.6) 2
    else 1
  }
}