package ma.fin.monitor.entity2risk

import ma.fin.monitor.common.entity.monitor.{EntityFrequency, AnomalyAlert}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

/**
 * 频率异常检测器
 */
class FrequencyAnomalyDetector(zScoreThreshold: Double = 2.5)
  extends KeyedProcessFunction[String, EntityFrequency, AnomalyAlert] {
  
  // 状态变量，保存历史频率数据
  private var frequencyState: ValueState[FrequencyStats] = _
  
  override def open(parameters: Configuration): Unit = {
    val frequencyStateDesc = new ValueStateDescriptor[FrequencyStats](
      "frequency-stats", classOf[FrequencyStats])
    frequencyState = getRuntimeContext.getState(frequencyStateDesc)
  }
  
  override def processElement(
      freq: EntityFrequency,
      ctx: KeyedProcessFunction[String, EntityFrequency, AnomalyAlert]#Context,
      out: Collector[AnomalyAlert]): Unit = {
    
    // 获取历史频率统计
    val stats = Option(frequencyState.value()).getOrElse(
      FrequencyStats(windowSize = 168)) // 保存1周的小时级数据
    
    // 添加当前观测值
    stats.addObservation(freq.count)
    
    // 至少需要24个观测值才能可靠地检测异常
    if (stats.observationCount >= 24) {
      // 计算Z分数
      val mean = stats.calculateMean()
      val stdDev = math.max(stats.calculateStdDev(), 1.0) // 避免除零
      val zScore = (freq.count - mean) / stdDev
      
      // 检测异常
      if (zScore > zScoreThreshold) {
        // 异常严重程度
        val severity = if (zScore > zScoreThreshold * 2) 5
                       else if (zScore > zScoreThreshold * 1.5) 4
                       else 3
        
        // 生成预警
        out.collect(AnomalyAlert(
          entity = freq.entity,
          alertType = "FREQUENCY_ANOMALY",
          severity = severity,
          zScore = zScore,
          observedValue = freq.count,
          expectedValue = mean,
          timestamp = ctx.timestamp(),
          description = s"Unusual activity detected for entity ${freq.entity}. " +
                        s"Observed frequency (${freq.count}) is ${zScore.toInt} standard " +
                        s"deviations above expected (${mean.toInt})."
        ))
      }
    }
    
    // 更新状态
    frequencyState.update(stats)
  }
}

/**
 * 频率统计类
 */
case class FrequencyStats(windowSize: Int = 168) {
  private val stats = new DescriptiveStatistics(windowSize)
  var observationCount: Int = 0
  
  // 添加观测值
  def addObservation(value: Long): Unit = {
    stats.addValue(value)
    observationCount += 1
  }
  
  // 计算均值
  def calculateMean(): Double = stats.getMean
  
  // 计算标准差
  def calculateStdDev(): Double = stats.getStandardDeviation
}