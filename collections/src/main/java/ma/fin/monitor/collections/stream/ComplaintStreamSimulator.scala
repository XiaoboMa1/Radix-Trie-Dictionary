package ma.fin.monitor.collections.stream

import java.io.{BufferedReader, FileReader}
import java.text.SimpleDateFormat
import java.util.concurrent.{Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean

import com.google.common.util.concurrent.RateLimiter
import com.google.gson.Gson
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
 * 投诉数据流模拟器 - 将CFPB静态CSV投诉数据转换为时间流
 */
class ComplaintStreamSimulator(
    csvFilePath: String,
    kafkaTopic: String,
    eventsPerSecond: Double = 5.0,  // 每秒投诉数，投诉比交易少
    timeScaleFactor: Double = 86400.0 // 投诉时间更慢，1天压缩到1秒
) {
  
  private val logger = LoggerFactory.getLogger(getClass)
  private val gson = new Gson()
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  private val running = new AtomicBoolean(true)
  private val producer = createKafkaProducer()
  private val rateLimiter = RateLimiter.create(eventsPerSecond)
  private val executor = Executors.newSingleThreadExecutor()
  
  /**
   * 开始模拟投诉流
   */
  def start(): Unit = {
    executor.submit(new Runnable {
      override def run(): Unit = {
        try {
          simulateComplaintStream()
        } catch {
          case e: Exception => 
            logger.error("投诉流模拟出错", e)
        }
      }
    })
    
    logger.info(s"开始以每秒${eventsPerSecond}条记录的速率模拟投诉流")
  }
  
  /**
   * 停止模拟
   */
  def stop(): Unit = {
    running.set(false)
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)
    producer.close()
    logger.info("投诉流模拟已停止")
  }
  
  /**
   * 模拟投诉流的核心方法
   */
  private def simulateComplaintStream(): Unit = {
    // 加载投诉数据
    val complaints = loadComplaints()
    
    // 按日期排序
    val sortedComplaints = complaints.sortBy(_.date)
    
    if (sortedComplaints.isEmpty) {
      logger.warn("没有可用的投诉数据")
      return
    }
    
    logger.info(s"加载了${sortedComplaints.size}条投诉记录，准备模拟流")
    
    // 记录前一条投诉时间，用于计算延迟
    var prevTimestamp = sortedComplaints.head.timestamp
    
    // 模拟流式处理
    for (complaint <- sortedComplaints if running.get()) {
      // 计算时间差（按比例缩放）
      val timeDiff = (complaint.timestamp - prevTimestamp) / timeScaleFactor
      if (timeDiff > 0) {
        Thread.sleep(timeDiff.toLong)
      }
      
      // 获取令牌（限流）
      rateLimiter.acquire()
      
      // 发送到Kafka
      val json = gson.toJson(complaint)
      producer.send(new ProducerRecord[String, String](
        kafkaTopic, complaint.complaintId, json))
      
      prevTimestamp = complaint.timestamp
    }
    
    producer.flush()
    logger.info("投诉流模拟完成")
  }
  
  /**
   * 从CSV加载投诉数据
   */
  private def loadComplaints(): Seq[ComplaintRecord] = {
    val complaints = new ArrayBuffer[ComplaintRecord]()
    val reader = new BufferedReader(new FileReader(csvFilePath))
    
    try {
      // 读取表头
      val header = reader.readLine()
      
      // 解析表头
      val headerFields = parseCSVLine(header)
      val dateIndex = headerFields.indexOf("date")
      val companyIndex = headerFields.indexOf("company")
      val issueTypeIndex = headerFields.indexOf("issue_type")
      val textIndex = headerFields.indexOf("specific_issue")
      val stateIndex = headerFields.indexOf("state")
      val complaintIdIndex = headerFields.indexOf("complaint_id")
      
      // 逐行读取投诉
      var line: String = null
      while ({line = reader.readLine(); line != null}) {
        try {
          val fields = parseCSVLine(line)
          if (fields.length > Math.max(dateIndex, Math.max(companyIndex, complaintIdIndex))) {
            val date = fields(dateIndex)
            val timestamp = dateFormat.parse(date).getTime
            
            val complaint = ComplaintRecord(
              date = date,
              timestamp = timestamp,
              issueCategory = if (fields.length > issueTypeIndex - 1) fields(issueTypeIndex - 1) else "",
              issueType = if (fields.length > issueTypeIndex) fields(issueTypeIndex) else "",
              text = if (fields.length > textIndex) fields(textIndex) else "",
              company = normalizeCompanyName(if (fields.length > companyIndex) fields(companyIndex) else ""),
              state = if (fields.length > stateIndex) fields(stateIndex) else "",
              complaintId = if (fields.length > complaintIdIndex) fields(complaintIdIndex) else java.util.UUID.randomUUID().toString
            )
            complaints += complaint
          }
        } catch {
          case e: Exception => 
            logger.warn(s"解析投诉记录失败: $line", e)
        }
      }
    } finally {
      reader.close()
    }
    
    complaints
  }
  
  /**
   * 解析CSV行，处理引号内的逗号
   */
  private def parseCSVLine(line: String): Array[String] = {
    val tokens = new ArrayBuffer[String]()
    var inQuotes = false
    val currentToken = new StringBuilder()
    
    for (c <- line) {
      if (c == '"') {
        inQuotes = !inQuotes
      } else if (c == ',' && !inQuotes) {
        tokens += currentToken.toString.trim
        currentToken.clear()
      } else {
        currentToken.append(c)
      }
    }
    tokens += currentToken.toString.trim
    
    tokens.toArray
  }
  
  /**
   * 创建Kafka生产者
   */
  private def createKafkaProducer(): KafkaProducer[String, String] = {
    val props = new java.util.Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("acks", "1")
    
    new KafkaProducer[String, String](props)
  }
  
  /**
   * 标准化公司名称
   */
  private def normalizeCompanyName(name: String): String = {
    if (name == null || name.isEmpty) return ""
    
    name.trim
      .toUpperCase
      .replaceAll("\\s+", " ")
      .replaceAll("\\bLTD\\b", "LIMITED")
      .replaceAll("\\bCORP\\b", "CORPORATION")
      .replaceAll("\\bINC\\b", "INCORPORATED")
  }
}

/**
 * 投诉记录数据结构 - 修改以匹配数据集
 */
case class ComplaintRecord(
  date: String,
  timestamp: Long,
  issueCategory: String,
  issueType: String,
  text: String,
  company: String,
  state: String,
  complaintId: String,
  var detectedTopic: String = ""  // 添加检测到的主题字段
)