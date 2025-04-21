package ma.fin.monitor.entity2dim.sink

import ma.fin.monitor.common.entity.financial.FinancialEntity
import ma.fin.monitor.common.transform.Sink
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.slf4j.{Logger, LoggerFactory}

/**
 * 金融实体Sink
 */
class FinancialEntitySink extends Sink[FinancialEntity] {
  override def doSink(input: DataStream[FinancialEntity]): Unit = {
    input.addSink(new FinancialEntitySinkFunction)
  }
}

class FinancialEntitySinkFunction extends RichSinkFunction[FinancialEntity] {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  
  override def invoke(entity: FinancialEntity, context: SinkFunction.Context): Unit = {
    try {
      // 实际处理金融实体的逻辑
      logger.info(s"Processing financial entity: ${entity.id}")
    } catch {
      case e: Exception =>
        logger.error(s"Failed to process financial entity: ${e.getMessage}", e)
    }
  }
}