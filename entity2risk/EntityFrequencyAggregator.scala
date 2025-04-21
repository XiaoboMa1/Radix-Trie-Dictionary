package ma.fin.monitor.entity2risk
// Add missing imports at the top:
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import ma.fin.monitor.common.entity.financial.EntityMention
import ma.fin.monitor.common.entity.monitor.EntityFrequency
import org.apache.flink.api.common.functions.AggregateFunction

/**
 * 实体频率聚合器
 */
class EntityFrequencyAggregator 
  extends AggregateFunction[EntityMention, Long, EntityFrequency] {
  
  // 创建累加器
  override def createAccumulator(): Long = 0L
  
  // 累加值
  override def add(mention: EntityMention, accumulator: Long): Long = accumulator + 1
  
  // 获取结果
  override def getResult(accumulator: Long): EntityFrequency = EntityFrequency(
    entity = null, // 将在WindowFunction中设置
    count = accumulator,
    windowStart = 0L, // 将在WindowFunction中设置
    windowEnd = 0L    // 将在WindowFunction中设置
  )
  
  // 合并累加器
  override def merge(a: Long, b: Long): Long = a + b
}

/**
 * 添加实体和窗口信息的窗口处理器
 */
class EntityFrequencyWindowFunction 
  extends ProcessWindowFunction[EntityFrequency, EntityFrequency, String, TimeWindow] {
  
  override def process(
      entity: String,
      context: Context,
      frequencies: Iterable[EntityFrequency],
      out: Collector[EntityFrequency]): Unit = {
    
    val frequency = frequencies.iterator.next()
    
    // 添加实体和窗口信息
    frequency.entity = entity
    frequency.windowStart = context.window.getStart
    frequency.windowEnd = context.window.getEnd
    
    out.collect(frequency)
  }
}