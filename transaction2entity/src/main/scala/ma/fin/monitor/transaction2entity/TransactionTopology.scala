package ma.fin.monitor.transaction2entity

import ma.fin.monitor.common.config.KafkaConfig
import ma.fin.monitor.common.entity.financial.{FinancialTransaction, EntityMention}
import ma.fin.monitor.common.sink.SinkFactory
import ma.fin.monitor.common.source.SourceFactory
import ma.fin.monitor.common.utils.Constants
import ma.fin.monitor.transaction2entity.transform.{TransactionEntityExtractor, MerchantRiskScorer}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import java.time.Duration
import org.slf4j.{Logger, LoggerFactory}
import ma.fin.monitor.transaction2entity.RadixTreeSetupSource

/**
 * 交易流处理拓扑
 */
object TransactionTopology {
    private val logger = LoggerFactory.getLogger(getClass)

    def build(kafkaConfig: KafkaConfig)(implicit env: StreamExecutionEnvironment): Unit = {
        // 配置RadixTree
        val radixTreeSetup = env.addSource(new RadixTreeSetupSource())
            .name("radix-tree-setup")

        // 从Kafka读取交易流
        val transactionStream = SourceFactory.createKafkaStream[FinancialTransaction](
            kafkaConfig, Constants.FINANCIAL_TRANSACTIONS_TOPIC)
            .assignTimestampsAndWatermarks(
                WatermarkStrategy
                    .forBoundedOutOfOrderness(Duration.ofMinutes(1))
                    .withTimestampAssigner(new SerializableTimestampAssigner[FinancialTransaction] {
                        override def extractTimestamp(element: FinancialTransaction, recordTimestamp: Long): Long = {
                            val ts = if (element.timestamp > 0) element.timestamp else element.ts
                            if (ts <= 0) {
                                logger.warn(s"Invalid timestamp found for transaction ${element.transaction_id}, using current time.")
                                System.currentTimeMillis()
                            } else {
                                ts
                            }
                        }
                    })
            )

        // 商户实体提取与分析
        val merchantStream = transactionStream
            .process(new TransactionEntityExtractor())
            .name("merchant-entity-extraction")

        // 对商户进行风险评分
        val merchantRiskStream = merchantStream
            .keyBy(_.entity)
            .window(SlidingEventTimeWindows.of(Time.days(7), Time.hours(12)))
            .process(new MerchantRiskScorer())
            .name("merchant-risk-scoring")

        // 输出实体提及到Kafka
        merchantRiskStream
            .filter(_.score > 0.3) // 只关注中高风险商户
            .addSink(SinkFactory.createKafkaProducer[EntityMention](Constants.ENTITY_MENTIONS_TOPIC))
            .name("entity-mention-sink")
    }
}