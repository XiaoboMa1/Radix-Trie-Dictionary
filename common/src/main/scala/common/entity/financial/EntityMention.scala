package ma.fin.monitor.common.entity.financial

/**
 * 实体提及类 - 表示在文本中发现的实体引用
 */
case class EntityMention(
  entity: String,           // 实体名称
  entityType: String,       // 实体类型
  position: Int,            // 文本中的位置
  context: String,          // 上下文文本
  timestamp: Long,          // 发现时间戳
  sourceType: String = "",  // 来源类型：COMPLAINT, TRANSACTION, NEWS
  sourceId: String = "",    // 来源ID
  confidence: Double = 1.0  // 置信度
) {
  
  // 获取标准化实体名称
  def normalizedEntityName: String = {
    entity.trim.toUpperCase
      .replaceAll("\\s+", " ")
      .replaceAll("\\bLTD\\b", "LIMITED")
      .replaceAll("\\bCORP\\b", "CORPORATION")
      .replaceAll("\\bINC\\b", "INCORPORATED")
  }
  
  // 获取简短上下文（用于显示）
  def shortContext: String = {
    if (context.length <= 100) context
    else {
      val startPos = Math.max(0, position - 50)
      val endPos = Math.min(context.length, position + 50)
      "..." + context.substring(startPos, endPos) + "..."
    }
  }
  
  override def toString = s"EntityMention($entity [$entityType] at $position)"
}

/**
 * 实体提及对伴生对象
 */
object EntityMention {
  def fromComplaint(
      entity: String,
      position: Int,
      complaintRecord: ComplaintRecord): EntityMention = {
    
    EntityMention(
      entity = entity,
      entityType = "COMPANY",
      position = position,
      context = complaintRecord.complaint_text,
      timestamp = complaintRecord.ts,
      sourceType = "COMPLAINT",
      sourceId = complaintRecord.complaint_id
    )
  }
  
  def fromTransaction(
      entity: String,
      transaction: FinancialTransaction): EntityMention = {
    
    EntityMention(
      entity = entity,
      entityType = "COMPANY",
      position = 0,
      context = s"Transaction ${transaction.transaction_id} of ${transaction.amount} ${transaction.currency}",
      timestamp = transaction.timestamp,
      sourceType = "TRANSACTION",
      sourceId = transaction.transaction_id
    )
  }
}