package ma.fin.monitor.entity2dim

import ma.fin.monitor.common.config.{Config, KafkaConfig}
import ma.fin.monitor.common.utils.Constants
import java.util.concurrent.TimeUnit
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._

/**
 * 启动参数有-mode earliest/latest/committed/timestamp -timestamp "2021-04-10 00:00:00"
 * -mode earliest从kafka topic中最早开始消费
 * -mode latest从kafka topic中最新开始消费
 * -mode committed从flink 上次commit的消息开始消费
 * -mode timestamp从指定时间戳开始消费
 * -timestamp 只适用于timestamp mode
 */

object App {
  val OPTION_MODE = "mode"
  val OPTION_TIMESTAMP = "timestamp"
  val GROUP_ID = "ods-dim"

  def main(args: Array[String]): Unit = {
    val parameterTool = ParameterTool.fromArgs(args)

    val mode = parameterTool.get(OPTION_MODE, Constants.CONSUMER_MODE_COMMITTED)
    val timestamp = parameterTool.get(OPTION_TIMESTAMP, "")

    implicit val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(120000)
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(20, TimeUnit.SECONDS)))
    env.setParallelism(1)

    val kafkaConfig = KafkaConfig(Config.kafkaBrokers, GROUP_ID, mode, timestamp)

    StreamTopology.build(kafkaConfig)

    env.execute("ods-dim-streaming-job")
  }
}