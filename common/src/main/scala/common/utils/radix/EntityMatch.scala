package ma.fin.monitor.common.utils.radix
/**
 * 实体匹配类 - 表示在文本中匹配到的实体
 * 由RadixTree匹配返回
 */
case class EntityMatch(
  entity: String,        // 实体名称
  entityType: String,    // 实体类型
  startPos: Int,         // 开始位置
  endPos: Int,           // 结束位置
  confidence: Double     // 置信度
) {
  
  // 获取实体长度
  def length: Int = endPos - startPos
  
  // 是否为完全匹配
  def isExactMatch: Boolean = confidence >= 0.99
  
  // 获取实体类型的可读描述
  def entityTypeDescription: String = entityType match {
    case "FINANCIAL_INSTITUTION" => "金融机构"
    case "CORPORATE_ENTITY" => "公司实体"
    case "GOVERNMENT_BODY" => "政府机构"
    case "INDIVIDUAL" => "个人"
    case "PEP" => "政治敏感人物"
    case "CURRENCY" => "货币"
    case "VESSEL" => "船只"
    case "AIRCRAFT" => "飞机"
    case "SANCTIONED_ENTITY" => "被制裁实体"
    case "HIGH_RISK_JURISDICTION" => "高风险司法管辖区"
    case "REGULATORY_TERM" => "监管术语"
    case "RISK_INDICATOR" => "风险指标"
    case _ => entityType
  }
  
  override def toString = s"EntityMatch($entity [$entityType] at $startPos-$endPos: $confidence)"
}

/**
 * 实体类型映射
 */
object EntityTypeMapper {
  // RadixTree C++枚举到Scala字符串的映射
  val typeMap: Map[Int, String] = Map(
    0 -> "GENERIC",
    1 -> "FINANCIAL_INSTITUTION",
    2 -> "CORPORATE_ENTITY",
    3 -> "GOVERNMENT_BODY",
    4 -> "INDIVIDUAL",
    5 -> "PEP",
    6 -> "CURRENCY",
    7 -> "VESSEL",
    8 -> "AIRCRAFT",
    9 -> "SANCTIONED_ENTITY",
    10 -> "HIGH_RISK_JURISDICTION",
    11 -> "REGULATORY_TERM",
    12 -> "RISK_INDICATOR"
  )
  
  // 从整数获取类型名称
  def getTypeName(typeCode: Int): String = {
    typeMap.getOrElse(typeCode, "UNKNOWN")
  }
  
  // 从名称获取类型代码
  def getTypeCode(typeName: String): Int = {
    typeMap.find(_._2 == typeName).map(_._1).getOrElse(0)
  }
}