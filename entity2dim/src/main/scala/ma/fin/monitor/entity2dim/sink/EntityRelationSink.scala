package ma.fin.monitor.entity2dim.sink

import ma.fin.monitor.common.entity.monitor.EntityRelation
import ma.fin.monitor.common.transform.Sink
import ma.fin.monitor.common.utils.{HBaseUtil, LoggerUtil}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.hadoop.hbase.{TableName}
import org.apache.hadoop.hbase.client.{Connection, Put, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.{Logger, LoggerFactory}

/**
 * 实体关系维度写入HBase
 */
class EntityRelationSink extends Sink[EntityRelation] {
  override def doSink(input: DataStream[EntityRelation]): Unit = {
    input.addSink(new EntityRelationSinkFunction)
  }
}

class EntityRelationSinkFunction extends RichSinkFunction[EntityRelation] {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var conn: Connection = _
  private var table: Table = _

  override def open(parameters: Configuration): Unit = {
    conn = HBaseUtil.getConn()
    table = conn.getTable(TableName.valueOf("dim:entity_relations"))
  }

  override def invoke(relation: EntityRelation, context: SinkFunction.Context): Unit = {
    try {
      // 使用source_entity和target_entity组合作为rowkey
      val rowKey = s"${relation.source_entity}:${relation.target_entity}"
      val put = new Put(Bytes.toBytes(rowKey))
      
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("source_entity"), Bytes.toBytes(relation.source_entity))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("target_entity"), Bytes.toBytes(relation.target_entity))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("relation_type"), Bytes.toBytes(relation.relation_type))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("strength"), Bytes.toBytes(relation.strength.toString))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("first_seen"), Bytes.toBytes(relation.first_seen.toString))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("last_seen"), Bytes.toBytes(relation.last_seen.toString))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("occurrence_count"), Bytes.toBytes(relation.occurrence_count.toString))
      
      if (relation.contexts != null) {
        put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("contexts"), Bytes.toBytes(relation.contexts))
      }
      
      table.put(put)
    } catch {
      case e: Exception => LoggerUtil.error(logger, e,
        s"Failed to write entity relation: $relation")
    }
  }

  override def close(): Unit = {
    if (table != null) table.close()
    if (conn != null) conn.close()
  }
}