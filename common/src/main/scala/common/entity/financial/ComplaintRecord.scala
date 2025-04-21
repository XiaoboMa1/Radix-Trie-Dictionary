package ma.fin.monitor.common.entity.financial

import ma.fin.monitor.common.entity.ods.OdsModel
import ma.fin.monitor.common.entity.ods.SqlType

import scala.collection.mutable

/**
 * 投诉记录类 - 表示金融投诉
 */
class ComplaintRecord extends OdsModel {
  // OdsModel基础字段
  override var database: String = _
  override var table: String = _
  override var ts: Long = _
  override var sqlType: SqlType.Value = _
  override var old: mutable.Map[String, String] = _
  var timestamp: Long = _
  
  // 投诉特有字段
  var complaint_id: String = _
  var date: String = _
  var issue_category: String = _
  var issue_type: String = _
  var specific_issue: String = _
  var complaint_text: String = _
  var company: String = _
  var state: String = _
  var zip_code: String = _
  var submission_channel: String = _
  var timely_response: String = _
  var response_status: String = _
  var consumer_disputed: String = _
  var severity_score: Double = 0.0 // 计算的严重程度
  
  override def toString = s"ComplaintRecord(id=$complaint_id, date=$date, " +
    s"company=$company, issue=$issue_type, text=$complaint_text)"
}

/**
 * 投诉记录伴生对象
 */
object ComplaintRecord {
  def apply(): ComplaintRecord = new ComplaintRecord()
  
  def apply(
      complaintId: String,
      date: String,
      company: String,
      issueType: String,
      text: String): ComplaintRecord = {
    
    val complaint = new ComplaintRecord()
    complaint.complaint_id = complaintId
    complaint.date = date
    complaint.company = company
    complaint.issue_type = issueType
    complaint.complaint_text = text
    complaint.ts = System.currentTimeMillis()
    
    complaint
  }
  
  /**
   * 从JSON解析
   */
  def fromJson(json: String): ComplaintRecord = {
    import com.google.gson.Gson
    val gson = new Gson()
    gson.fromJson(json, classOf[ComplaintRecord])
  }
}