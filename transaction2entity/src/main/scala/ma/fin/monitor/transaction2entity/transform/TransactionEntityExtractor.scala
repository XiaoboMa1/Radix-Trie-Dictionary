package ma.fin.monitor.transaction2entity.transform

import ma.fin.monitor.common.entity.financial.{FinancialTransaction, EntityMention}
import ma.fin.monitor.common.utils.radix.{RadixTreeJNI, EntityMatch}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

/**
 * 从交易数据中提取实体
 */
class TransactionEntityExtractor extends ProcessFunction[FinancialTransaction, EntityMention] {
  @transient private var radixTree: RadixTreeJNI = _
  @transient private var treeHandle: Long = _
  
  override def open(parameters: Configuration): Unit = {
    // 初始化RadixTree
    radixTree = new RadixTreeJNI()
    val dictPath = getRuntimeContext.getDistributedCache
      .getFile("sanctions_dictionary.txt").getAbsolutePath
    treeHandle = radixTree.initialize(dictPath)
  }
  
  override def processElement(
      transaction: FinancialTransaction,
      ctx: ProcessFunction[FinancialTransaction, EntityMention]#Context,
      out: Collector[EntityMention]): Unit = {
    
    // 首先提取商户名称作为实体
    out.collect(EntityMention(
      entity = transaction.receiver_id,
      entityType = "MERCHANT",
      position = 0,
      context = s"Transaction ${transaction.transaction_id}: ${transaction.amount} ${transaction.currency}",
      category = "TRANSACTION",
      timestamp = transaction.timestamp
    ))
    
    // 如果交易描述存在，从中提取实体
    if (transaction.description != null && !transaction.description.isEmpty) {
      val entities = radixTree.findEntities(treeHandle, transaction.description)
      
      // 输出找到的所有实体
      entities.foreach { entity =>
        val contextStart = math.max(0, entity.startPos - 20)
        val contextEnd = math.min(transaction.description.length, entity.endPos + 20)
        val context = transaction.description.substring(contextStart, contextEnd)
        
        out.collect(EntityMention(
          entity = entity.entity,
          entityType = entity.entityType,
          position = entity.startPos,
          context = context,
          category = "TRANSACTION_DESC",
          timestamp = transaction.timestamp
        ))
      }
    }
  }
  
  override def close(): Unit = {
    if (treeHandle != 0) {
      radixTree.dispose(treeHandle)
    }
  }
}