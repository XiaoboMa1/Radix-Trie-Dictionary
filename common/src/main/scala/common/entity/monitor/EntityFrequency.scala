package ma.fin.monitor.common.entity.monitor

import java.text.SimpleDateFormat
import java.util.Date

/**
 * 实体频率类 - 记录实体在时间窗口中的出现频率
 */
case class EntityFrequency(
  entity: String,         // 实体名称
  count: Long,            // 出现次数
  windowStart: Long,      // 窗口开始时间
  windowEnd: Long,        // 窗口结束时间
  entityType: String = "" // 实体类型
) {
  
  // 获取窗口大小（毫秒）
  def windowSize: Long = windowEnd - windowStart
  
  // 获取频率（每小时）
  def hourlyRate: Double = {
    val hourMillis = 3600000.0
    count * hourMillis / windowSize
  }
  
  // 获取格式化的窗口时间范围
  def formattedTimeRange: String = {
    val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    s"${fmt.format(new Date(windowStart))} to ${fmt.format(new Date(windowEnd))}"
  }
  
  override def toString = s"EntityFrequency($entity: $count times in $formattedTimeRange)"
}

/**
 * 实体频率统计
 * 用于存储实体频率历史记录，计算统计值
 */
class FrequencyStats(windowSize: Int = 100) {
  private val observations = new Array[Double](windowSize)
  private var index = 0
  private var sum = 0.0
  private var sumSquares = 0.0
  var observationCount = 0
  
  /**
   * 添加新的观察值
   */
  def addObservation(value: Double): Unit = {
    // 更新合计
    sum += value
    sumSquares += value * value
    
    // 如果已经填满数组，需要减去被替换的值
    if (observationCount >= windowSize) {
      val oldValue = observations(index)
      sum -= oldValue
      sumSquares -= oldValue * oldValue
    } else {
      observationCount += 1
    }
    
    // 存储新值
    observations(index) = value
    index = (index + 1) % windowSize
  }
  
  /**
   * 计算平均值
   */
  def calculateMean(): Double = {
    if (observationCount == 0) 0.0 else sum / observationCount
  }
  
  /**
   * 计算标准差
   */
  def calculateStdDev(): Double = {
    if (observationCount <= 1) return 0.0
    
    val mean = calculateMean()
    val variance = (sumSquares / observationCount) - (mean * mean)
    Math.sqrt(Math.max(0.0, variance))
  }
  
  /**
   * 计算Z分数
   */
  def calculateZScore(value: Double): Double = {
    val mean = calculateMean()
    val stdDev = calculateStdDev()
    
    if (stdDev == 0.0) {
      if (value > mean) 3.0 else 0.0
    } else {
      (value - mean) / stdDev
    }
  }
  
  override def toString = s"FrequencyStats(n=$observationCount, mean=${calculateMean()}, " +
    s"stdDev=${calculateStdDev()})"
}