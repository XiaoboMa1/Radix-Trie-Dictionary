package ma.fin.monitor.entity2dim.sink

import ma.fin.monitor.common.config.KafkaConfig
import ma.fin.monitor.common.entity.financial.{ComplaintRecord, EntityMention}
import ma.fin.monitor.common.sink.SinkFactory
import ma.fin.monitor.common.source.SourceFactory
import ma.fin.monitor.common.utils.Constants
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import java.time.Duration
import org.slf4j.{Logger, LoggerFactory}

/**
 * 投诉流处理拓扑
 */
object ComplaintTopology {
  private val logger = LoggerFactory.getLogger(getClass)

  def build(kafkaConfig: KafkaConfig)(implicit env: StreamExecutionEnvironment): Unit = {
    // 从Kafka读取投诉流
    val complaintStream = SourceFactory.createKafkaStream[ComplaintRecord](
      kafkaConfig, Constants.FINANCIAL_COMPLAINTS_TOPIC)
      .assignTimestampsAndWatermarks(
        WatermarkStrategy
          .forBoundedOutOfOrderness(Duration.ofMinutes(1))
          .withTimestampAssigner(new SerializableTimestampAssigner[ComplaintRecord] {
            override def extractTimestamp(element: ComplaintRecord, recordTimestamp: Long): Long = {
              // Check if timestamp is set, otherwise use ts field or current time as fallback
              val timestamp = if (element.timestamp > 0) {
                element.timestamp
              } else if (element.ts > 0) {
                element.ts
              } else {
                System.currentTimeMillis()
              }
              
              logger.debug(s"Extracted timestamp: $timestamp from complaint: ${element.complaint_id}")
              timestamp
            }
          })
      )
    
    // Log the stream for debugging
    complaintStream.print("Complaint Record")
    
    // 投诉主题分类 - 使用transaction2entity中的类
    val classifiedComplaintStream = complaintStream
      .process(new ma.fin.monitor.transaction2entity.transform.ComplaintTopicClassifier())
      .name("complaint-classification")
    
    // 投诉实体提取 - 使用transaction2entity中的类
    val complaintEntityStream = classifiedComplaintStream
      .process(new ma.fin.monitor.transaction2entity.transform.ComplaintEntityExtractor())
      .name("complaint-entity-extraction")
    
    // 投诉实体聚合 - 使用transaction2entity中的类
    val complaintEntitySummary = complaintEntityStream
      .keyBy(mention => mention.entity)
      .window(TumblingEventTimeWindows.of(Time.days(1)))
      .aggregate(new ma.fin.monitor.transaction2entity.transform.ComplaintSummaryAggregator())
      .name("complaint-entity-aggregation")
    
    // 输出实体提及到Kafka
    complaintEntitySummary
      .addSink(SinkFactory.createKafkaProducer[EntityMention](Constants.ENTITY_MENTIONS_TOPIC))
      .name("entity-mention-sink")
  }
}