package ma.fin.monitor.transaction2entity

import ma.fin.monitor.common.config.KafkaConfig
import ma.fin.monitor.common.entity.financial.{ComplaintRecord, EntityMention}
import ma.fin.monitor.common.sink.SinkFactory
import ma.fin.monitor.common.source.SourceFactory
import ma.fin.monitor.common.utils.Constants
import ma.fin.monitor.transaction2entity.transform.{ComplaintEntityExtractor, ComplaintTopicClassifier}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import java.time.Duration
import org.slf4j.{Logger, LoggerFactory}
import ma.fin.monitor.transaction2entity.RadixTreeSetupSource


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
                            val ts = if (element.timestamp > 0) element.timestamp else element.ts
                            if (ts <= 0) {
                                logger.warn(s"Invalid timestamp found for complaint ${element.complaint_id}, using current time.")
                                System.currentTimeMillis()
                            } else {
                                ts
                            }
                        }
                    })
            )
        
        // 投诉主题分类
        val classifiedComplaintStream = complaintStream
            .process(new ComplaintTopicClassifier())
            .name("complaint-classification")
        
        // 投诉实体提取
        val complaintEntityStream = classifiedComplaintStream
            .process(new ComplaintEntityExtractor())
            .name("complaint-entity-extraction")
        
        // 投诉实体聚合
        val complaintEntitySummary = complaintEntityStream
            .keyBy(mention => (mention.entity, mention.category))
            .window(TumblingEventTimeWindows.of(Time.days(1)))
            .aggregate(new ComplaintSummaryAggregator())
            .name("complaint-entity-aggregation")
        
        // 输出实体提及到Kafka
        complaintEntitySummary
            .addSink(SinkFactory.createKafkaProducer[EntityMention](Constants.ENTITY_MENTIONS_TOPIC))
            .name("entity-mention-sink")
        
        // Example: Log the stream
        complaintStream.print("Complaint Stream")
    }
}