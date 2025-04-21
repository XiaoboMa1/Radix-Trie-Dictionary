package ma.fin.monitor.entity2risk

import java.util.concurrent.TimeUnit

import ma.fin.monitor.common.Config{Config, KafkaConfig}
import ma.fin.monitor.common.utils.Constants
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._

/**
 * 实体风险分析主程序
 */
object App {
  val OPTION_MODE = "mode"
  val OPTION_TIMESTAMP = "timestamp"
  val GROUP_ID = "entity-risk"

  def main(args: Array[String]): Unit = {
    val parameterTool = ParameterTool.fromArgs(args)

    val mode = parameterTool.get(OPTION_MODE, Constants.CONSUMER_MODE_COMMITTED)
    val timestamp = parameterTool.get(OPTION_TIMESTAMP, "")

    implicit val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(120000)
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(20, TimeUnit.SECONDS)))
    env.setParallelism(4)

    val kafkaConfig = KafkaConfig(Config.kafkaBrokers, GROUP_ID, mode, timestamp)

    // 构建风险分析拓扑
    RiskAnalysisTopology.build(kafkaConfig)

    env.execute("entity-risk-job")
  }
}