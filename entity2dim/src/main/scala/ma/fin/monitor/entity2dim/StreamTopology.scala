package ma.fin.monitor.entity2dim

import ma.fin.monitor.common.config.{Config, KafkaConfig}
import ma.fin.monitor.common.entity.financial.{ComplaintRecord, FinancialEntity}
import ma.fin.monitor.common.source.SourceFactory
import ma.fin.monitor.common.utils.Constants
import ma.fin.monitor.entity2dim.sink.{ComplaintSink, FinancialEntitySink}
import org.apache.flink.streaming.api.scala._

/**
 * 流式拓扑构建类 - 处理实体维度数据
 */
object StreamTopology {
  
  def build(kafkaConfig: KafkaConfig)(implicit env: StreamExecutionEnvironment): Unit = {
    // 从Kafka读取投诉流
    val complaintStream = SourceFactory.createKafkaStream[ComplaintRecord](
      kafkaConfig, Constants.FINANCIAL_COMPLAINTS_TOPIC)
      
    // 从Kafka读取实体流
    val financialEntityStream = SourceFactory.createKafkaStream[FinancialEntity](
      kafkaConfig, Constants.ENTITY_MENTIONS_TOPIC)

    /**
     * 维度数据写入HBase
     */
    val complaintSink = new ComplaintSink()
    val financialEntitySink = new FinancialEntitySink()
    complaintSink.doSink(complaintStream)
    financialEntitySink.doSink(financialEntityStream)
  }
}