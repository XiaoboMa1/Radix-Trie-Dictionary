package ma.fin.monitor.common.entity.monitor

import java.text.SimpleDateFormat
import java.util.Date

/**
 * 实体关系类 - 表示两个实体之间的关系
 */
case class EntityRelation(
  source_entity: String,    // 源实体ID
  target_entity: String,    // 目标实体ID
  relation_type: String,    // 关系类型
  strength: Double,         // 关系强度
  first_seen: Long,         // 首次发现时间戳
  last_seen: Long,          // 最后发现时间戳
  occurrence_count: Int,    // 共现次数
  contexts: String          // 关系上下文
) {
  
  // 获取格式化的首次发现时间
  def formattedFirstSeen: String = {
    val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    fmt.format(new Date(first_seen))
  }
  
  // 获取格式化的最后发现时间
  def formattedLastSeen: String = {
    val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    fmt.format(new Date(last_seen))
  }
  
  // 获取关系持续时间（毫秒）
  def duration: Long = last_seen - first_seen
  
  // 获取格式化的关系强度（百分比）
  def strengthPercentage: String = {
    f"${strength * 100}%.1f%%"
  }
  
  override def toString = s"EntityRelation($source_entity → $target_entity [$relation_type]: " +
    s"strength=${strengthPercentage}, occurrences=$occurrence_count, " +
    s"duration=${duration / (1000 * 60 * 60 * 24)}d)"
}

/**
 * 实体对 - 用于中间计算
 */
case class EntityPair(
  entity1: String,
  entity2: String,
  context: String,
  timestamp: Long
)