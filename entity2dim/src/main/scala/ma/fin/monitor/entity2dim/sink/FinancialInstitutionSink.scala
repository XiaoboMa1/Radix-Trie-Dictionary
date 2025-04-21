package ma.fin.monitor.entity2dim.sink

import ma.fin.monitor.common.entity.financial.FinancialEntity
import ma.fin.monitor.common.transform.Sink
import ma.fin.monitor.common.utils.{Constants, HBaseUtil, LoggerUtil}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{Connection, Put, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.{Logger, LoggerFactory}

/**
 * 金融机构实体维度表写入HBase
 */
class FinancialInstitutionSink extends Sink[FinancialEntity] {
  override def doSink(input: DataStream[FinancialEntity]): Unit =
    input.addSink(new FinancialInstitutionSinkFunction)
}

class FinancialInstitutionSinkFunction extends RichSinkFunction[FinancialEntity] {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var conn: Connection = _
  private var table: Table = _

  override def open(parameters: Configuration): Unit = {
    conn = HBaseUtil.getConn()
    table = conn.getTable(TableName.valueOf("dim:financial_institutions"))
  }

  override def invoke(entity: FinancialEntity, context: SinkFunction.Context): Unit =
    try {
      val put = new Put(Bytes.toBytes(entity.id))

      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("entity_name"), Bytes.toBytes(entity.entity_name))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("jurisdiction"), Bytes.toBytes(entity.jurisdiction))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("risk_level"), Bytes.toBytes(entity.risk_level))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("first_seen_date"), Bytes.toBytes(entity.first_seen_date))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("is_sanctioned"), Bytes.toBytes(entity.is_sanctioned.toString))

      if (entity.sanctions_detail != null) {
        put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("sanctions_detail"), Bytes.toBytes(entity.sanctions_detail))
      }

      table.put(put)
    } catch {
      case e: Exception => LoggerUtil.error(logger, e,
        s"Failed to write financial institution: $entity")
    }

  override def close(): Unit = {
    if (table != null) table.close()
    if (conn != null) conn.close()
  }
}