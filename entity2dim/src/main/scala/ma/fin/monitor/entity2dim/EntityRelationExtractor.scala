package ma.fin.monitor.entity2dim

import ma.fin.monitor.common.entity.financial.EntityMention
import ma.fin.monitor.common.entity.monitor.EntityRelation
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.util.Collector
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction

import scala.collection.mutable
import scala.collection.JavaConverters._

/**
 * 从实体提及中提取实体关系
 */
class EntityRelationExtractor extends ProcessWindowFunction[
    EntityMention, EntityRelation, String, TimeWindow] {
  
  override def process(
      key: String,
      context: Context,
      elements: Iterable[EntityMention],
      out: Collector[EntityRelation]): Unit = {
    
    // 获取共现的所有实体
    val mentions = elements.toList
    
    // 如果同一上下文至少有两个实体，则构建关系
    if (mentions.size >= 2) {
      // 构建实体对
      for (i <- 0 until mentions.size - 1) {
        for (j <- i + 1 until mentions.size) {
          val entity1 = mentions(i).entity
          val entity2 = mentions(j).entity
          
          // 创建实体关系
          out.collect(EntityRelation(
            source_entity = entity1, 
            target_entity = entity2,
            relation_type = "CO_OCCURRENCE",
            strength = 1.0, // 初始强度，后续聚合会计算真实强度
            first_seen = context.window.getStart,
            last_seen = context.window.getEnd,
            occurrence_count = 1,
            contexts = key
          ))
        }
      }
    }
  }
}