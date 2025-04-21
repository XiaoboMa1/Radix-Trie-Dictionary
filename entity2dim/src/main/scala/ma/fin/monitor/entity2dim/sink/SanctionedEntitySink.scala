package ma.fin.monitor.entity2dim.sink

import ma.fin.monitor.common.entity.financial.FinancialEntity
import ma.fin.monitor.common.transform.Sink
import ma.fin.monitor.common.utils.{Constants, HBaseUtil, LoggerUtil, JedisWrapper, JedisConnectionPool}
import ma.fin.monitor.common.config.RedisConfig
import ma.fin.monitor.common.config.Config
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{Connection, Put, Table}
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.{Logger, LoggerFactory}


/**
 * 制裁实体同时写入HBase和Redis
 * Redis用于快速查询，HBase用于存储完整细节
 */
class SanctionedEntitySink extends Sink[FinancialEntity] {
  override def doSink(input: DataStream[FinancialEntity]): Unit =
    input.addSink(new SanctionedEntitySinkFunction)
}

class SanctionedEntitySinkFunction extends RichSinkFunction[FinancialEntity] {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var conn: Connection = _
  private var table: Table = _
  private implicit val redisConfig = RedisConfig(
    Config.redisHost, Config.redisPort, Config.redisPassword, Config.redisDb)

  override def open(parameters: Configuration): Unit = {
    conn = HBaseUtil.getConn()
    table = conn.getTable(TableName.valueOf("dim:sanctioned_entities"))
  }

  override def invoke(entity: FinancialEntity, context: SinkFunction.Context): Unit =
    try {
      // 写入HBase
      val put = new Put(Bytes.toBytes(entity.id))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("entity_name"), Bytes.toBytes(entity.entity_name))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("entity_type"), Bytes.toBytes(entity.entity_type))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("jurisdiction"), Bytes.toBytes(entity.jurisdiction))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("sanctions_detail"), Bytes.toBytes(entity.sanctions_detail))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("source"), Bytes.toBytes(entity.source))
      put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("first_seen_date"), Bytes.toBytes(entity.first_seen_date))
      table.put(put)
      
      // 同时写入Redis，用于快速查询
      JedisWrapper.wrap(jedis => {
        // 使用Hash结构存储
        val key = s"sanctioned:entity:${entity.id}"
        jedis.hset(key, "name", entity.entity_name)
        jedis.hset(key, "type", entity.entity_type)
        jedis.hset(key, "source", entity.source)
        
        // 设置90天过期
        jedis.expire(key, 90 * 24 * 60 * 60L)
        
        // 同时设置一个名称索引，用于模糊匹配
        jedis.sadd("sanctioned:names", entity.entity_name.toLowerCase())
      }, s"Failed to write sanctioned entity to Redis: $entity")
    } catch {
      case e: Exception => LoggerUtil.error(logger, e,
        s"Failed to write sanctioned entity: $entity")
    }

  override def close(): Unit = {
    if (table != null) table.close()
    if (conn != null) conn.close()
  }
}