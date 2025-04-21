package ma.fin.monitor.common.source

import java.sql.Timestamp
import java.util.{Objects, Properties}

import com.alibaba.fastjson.JSON
import com.alibaba.otter.canal.protocol.FlatMessage
import ma.fin.monitor.common.config.KafkaConfig
import ma.fin.monitor.common.source.binlog.OdsModelFactory
import ma.fin.monitor.common.utils.{Constants, GsonUtil, LoggerUtil}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.slf4j.{Logger, LoggerFactory}

import scala.reflect.ClassTag

/**
 * 创建一个SourceFunction
 */
object SourceFactory extends Serializable {

  lazy val LOGGER: Logger = LoggerFactory.getLogger(this.getClass)

  def createKafkaStream[T: TypeInformation](kafkaConfig: KafkaConfig,
                                            topic: String
                                           )(implicit env: StreamExecutionEnvironment, real: ClassTag[T]): DataStream[T] = {
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", kafkaConfig.brokers)
    properties.setProperty("group.id", kafkaConfig.groupId)

    val kafkaConsumer = new FlinkKafkaConsumer[String](topic, new SimpleStringSchema(), properties)

    kafkaConfig.mode match {
      case Constants.CONSUMER_MODE_EARLIEST => kafkaConsumer.setStartFromEarliest()
      case Constants.CONSUMER_MODE_LATEST => kafkaConsumer.setStartFromLatest()
      case Constants.CONSUMER_MODE_TIMESTAMP => kafkaConsumer.setStartFromTimestamp(
        Timestamp.valueOf(kafkaConfig.timestamp).getTime)
      case _ =>
    }

    def parse(item: String): T = try {
      //      JSON.parseObject(item, real.runtimeClass).asInstanceOf[T]
      GsonUtil.gson.fromJson(item, real.runtimeClass).asInstanceOf[T]
    } catch {
      case _: Throwable => LOGGER.warn(s"failed to parse log item, log: $item")
        null.asInstanceOf[T]
    }

    env.addSource[String](kafkaConsumer).name("source:" + topic)
      .map(r => parse(r))
  }

  def createBinlogStream[T: TypeInformation](kafkaConfig: KafkaConfig,
                                             topic: String
                                            )(implicit env: StreamExecutionEnvironment, real: ClassTag[T]): DataStream[T] = {
    // 不需要DDL语句的binlog
    val flatMsgStream = createKafkaStream[FlatMessage](kafkaConfig, topic).filter(r => r.getData != null)

    flatMsgStream.flatMap(r => {
      var a = Seq[Any]()
      try {
        a = OdsModelFactory.build(r)
      } catch {
        case e: Exception => LoggerUtil.error(LOGGER, e,
          s"failed to build ods model, input=${r}")
      }
      a.asInstanceOf[Seq[T]]
    }).uid(s"ods_${real.runtimeClass.getSimpleName}")

  }

}
