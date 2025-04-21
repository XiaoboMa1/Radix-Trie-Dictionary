package ma.fin.monitor.common.entity.financial

import ma.fin.monitor.common.entity.ods.OdsModel
import ma.fin.monitor.common.entity.ods.SqlType

import scala.collection.mutable

/**
 * 金融实体类 - 替代原UserInfo模型
 * 表示金融机构、个人或其他实体
 */
class FinancialEntity extends OdsModel {
  // OdsModel基础字段
  override var database: String = _
  override var table: String = _
  override var ts: Long = _
  override var sqlType: SqlType.Value = _
  override var old: mutable.Map[String, String] = _
  
  // 实体特有字段
  var id: String = _
  var entity_name: String = _
  var entity_type: String = _ // FINANCIAL_INSTITUTION, INDIVIDUAL, VESSEL等
  var risk_level: String = _ // HIGH, MEDIUM, LOW
  var jurisdiction: String = _ // UK, US, EU等
  var first_seen_date: String = _
  var is_sanctioned: Boolean = false
  var sanctions_detail: String = _ // 制裁详情
  var source: String = _ // 数据来源
  var aliases: Array[String] = _ // 别名列表
  var metadata: mutable.Map[String, String] = mutable.Map.empty // 附加元数据
  
  /**
   * 从制裁列表数据创建实体
   */
  def fromSanctionsData(
      id: String,
      name: String,
      entityType: String,
      jurisdiction: String,
      sanctionsDetail: String,
      source: String): FinancialEntity = {
    
    this.id = id
    this.entity_name = name
    this.entity_type = entityType
    this.jurisdiction = jurisdiction
    this.is_sanctioned = true
    this.sanctions_detail = sanctionsDetail
    this.source = source
    this.risk_level = "HIGH" // 被制裁实体默认高风险
    
    this
  }
  
  override def toString = s"FinancialEntity(id=$id, name=$entity_name, type=$entity_type, " +
    s"risk_level=$risk_level, jurisdiction=$jurisdiction, is_sanctioned=$is_sanctioned, " +
    s"source=$source)"
}

/**
 * 金融实体伴生对象
 */
object FinancialEntity {
  def apply(): FinancialEntity = new FinancialEntity()
  
  def apply(
      id: String,
      name: String,
      entityType: String,
      jurisdiction: String = "",
      riskLevel: String = "MEDIUM"): FinancialEntity = {
    
    val entity = new FinancialEntity()
    entity.id = id
    entity.entity_name = name
    entity.entity_type = entityType
    entity.jurisdiction = jurisdiction
    entity.risk_level = riskLevel
    entity.is_sanctioned = false
    
    entity
  }
}