package ma.fin.monitor.common.entity.financial

import ma.fin.monitor.common.entity.ods.OdsModel
import ma.fin.monitor.common.entity.ods.SqlType

import scala.collection.mutable

/**
 * 金融交易类 - 替代原OrderInfo模型
 * 表示金融机构间的交易或资金转移
 */
class FinancialTransaction extends OdsModel {
  // OdsModel基础字段
  override var database: String = _
  override var table: String = _
  override var ts: Long = _
  override var sqlType: SqlType.Value = _
  override var old: mutable.Map[String, String] = _
  
  // 交易特有字段
  var transaction_id: String = _
  var sender_id: String = _
  var sender_name: String = _
  var receiver_id: String = _
  var receiver_name: String = _
  var amount: Double = _
  var currency: String = _
  var timestamp: Long = _
  var transaction_type: String = _ // PAYMENT, TRANSFER, WITHDRAWAL
  var risk_score: Double = 0.0
  var is_suspicious: Boolean = false
  var location_data: String = _ // JSON格式的位置信息
  var merchant_category: String = _
  var device_info: String = _
  var ip_address: String = _
  var card_number_hash: String = _ // 信用卡号哈希
  
  /**
   * 从信用卡交易数据转换
   */
  def fromCreditCardTransaction(
      transactionId: String,
      cardNum: String,
      timestamp: Long,
      amount: Double,
      merchant: String,
      category: String,
      merchantLat: Double,
      merchantLong: Double): FinancialTransaction = {
    
    this.transaction_id = transactionId
    this.receiver_id = merchant
    this.receiver_name = merchant
    this.amount = amount
    this.currency = "GBP" // 假设英国交易
    this.timestamp = timestamp
    this.transaction_type = "CARD_PAYMENT"
    this.merchant_category = category
    this.card_number_hash = hashCardNumber(cardNum)
    
    // 保存位置数据为JSON
    this.location_data = s"""{"lat":$merchantLat,"long":$merchantLong}"""
    
    this
  }
  
  /**
   * 哈希信用卡号，仅保留后4位
   */
  private def hashCardNumber(cardNum: String): String = {
    if (cardNum == null || cardNum.length < 4) return "****"
    
    val last4 = cardNum.takeRight(4)
    s"XXXX-XXXX-XXXX-$last4"
  }
  
  override def toString = s"FinancialTransaction(id=$transaction_id, " +
    s"sender=$sender_name, receiver=$receiver_name, amount=$amount $currency, " +
    s"type=$transaction_type, risk_score=$risk_score, is_suspicious=$is_suspicious)"
}

/**
 * 金融交易伴生对象
 */
object FinancialTransaction {
  def apply(): FinancialTransaction = new FinancialTransaction()
  
  def apply(
      transactionId: String,
      senderId: String,
      receiverId: String,
      amount: Double,
      currency: String,
      transactionType: String): FinancialTransaction = {
    
    val transaction = new FinancialTransaction()
    transaction.transaction_id = transactionId
    transaction.sender_id = senderId
    transaction.receiver_id = receiverId
    transaction.amount = amount
    transaction.currency = currency
    transaction.transaction_type = transactionType
    transaction.timestamp = System.currentTimeMillis()
    
    transaction
  }
}