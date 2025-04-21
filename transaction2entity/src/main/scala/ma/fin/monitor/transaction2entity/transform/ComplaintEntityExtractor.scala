package ma.fin.monitor.transaction2entity.transform

import ma.fin.monitor.common.entity.financial.{ComplaintRecord, EntityMention}
import ma.fin.monitor.common.utils.radix.{RadixTreeJNI, EntityMatch}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector
import org.slf4j.LoggerFactory

/**
 * 从投诉数据中提取实体
 */
class ComplaintEntityExtractor extends ProcessFunction[ComplaintRecord, EntityMention] {
  
  private val logger = LoggerFactory.getLogger(getClass)
  @transient private var radixTree: RadixTreeJNI = _
  @transient private var treeHandle: Long = _
  
  override def open(parameters: Configuration): Unit = {
    try {
      // 初始化RadixTree
      radixTree = new RadixTreeJNI()
      val dictPath = getRuntimeContext.getDistributedCache
        .getFile("sanctions_dictionary.txt").getAbsolutePath
      treeHandle = radixTree.initialize(dictPath)
      logger.info(s"RadixTree初始化完成，字典路径: $dictPath")
    } catch {
      case e: Exception => 
        logger.error("RadixTree初始化失败", e)
        throw e
    }
  }
  
  override def processElement(
      complaint: ComplaintRecord,
      ctx: ProcessFunction[ComplaintRecord, EntityMention]#Context,
      out: Collector[EntityMention]): Unit = {
    
    try {
      // 首先提取投诉中的公司名称作为实体
      out.collect(EntityMention(
        entity = complaint.company,
        entityType = "FINANCIAL_INSTITUTION",
        position = 0,
        context = complaint.issueType,
        timestamp = complaint.timestamp,
        sourceType = "COMPLAINT",
        sourceId = complaint.complaintId,
        category = complaint.detectedTopic
      ))
      
      // 从投诉文本中提取实体
      if (complaint.text != null && !complaint.text.isEmpty) {
        val entities = radixTree.findEntities(treeHandle, complaint.text)
        
        // 输出找到的所有实体
        entities.foreach { entity =>
          val contextStart = math.max(0, entity.startPos - 50)
          val contextEnd = math.min(complaint.text.length, entity.endPos + 50)
          val context = complaint.text.substring(contextStart, contextEnd)
          
          out.collect(EntityMention(
            entity = entity.entity,
            entityType = entity.entityType,
            position = entity.startPos,
            context = context,
            timestamp = complaint.timestamp,
            sourceType = "COMPLAINT_TEXT",
            sourceId = complaint.complaintId,
            confidence = entity.confidence,
            category = complaint.detectedTopic
          ))
        }
      }
    } catch {
      case e: Exception => 
        logger.error(s"实体提取失败，投诉ID: ${complaint.complaintId}", e)
    }
  }
  
  override def close(): Unit = {
    if (treeHandle != 0) {
      try {
        radixTree.dispose(treeHandle)
        logger.info("RadixTree资源已释放")
      } catch {
        case e: Exception => 
          logger.warn("关闭RadixTree时出错", e)
      }
    }
  }
}