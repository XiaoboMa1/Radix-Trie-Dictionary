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
 * 交易流模拟器 - 将静态CSV交易数据转换为时间流
 */
class TransactionStreamSimulator(
    csvFilePath: String,
    kafkaTopic: String,
    eventsPerSecond: Double = 10.0,
    timeScaleFactor: Double = 1000.0 // 加速因子，1000表示现实世界1秒对应模拟的1毫秒
) {
  
  private val logger = LoggerFactory.getLogger(getClass)
  private val gson = new Gson()
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private val running = new AtomicBoolean(true)
  private val producer = createKafkaProducer()
  private val rateLimiter = RateLimiter.create(eventsPerSecond)
  private val executor = Executors.newSingleThreadExecutor()
  
  /**
   * 开始模拟交易流
   */
  def start(): Unit = {
    executor.submit(new Runnable {
      override def run(): Unit = {
        try {
          simulateTransactionStream()
        } catch {
          case e: Exception => 
            logger.error("交易流模拟出错", e)
        }
      }
    })
    
    logger.info(s"开始以每秒${eventsPerSecond}条记录的速率模拟交易流")
  }
  
  /**
   * 停止模拟
   */
  def stop(): Unit = {
    running.set(false)
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)
    producer.close()
    logger.info("交易流模拟已停止")
  }
  
  /**
   * 模拟交易流的核心方法
   */
  private def simulateTransactionStream(): Unit = {
    // 加载交易数据
    val transactions = loadTransactions()
    
    // 按时间戳排序
    val sortedTransactions = transactions.sortBy(_.timestamp)
    
    if (sortedTransactions.isEmpty) {
      logger.warn("没有可用的交易数据")
      return
    }
    
    logger.info(s"加载了${sortedTransactions.size}条交易记录，准备模拟流")
    
    // 记录前一笔交易时间，用于计算延迟
    var prevTimestamp = sortedTransactions.head.timestamp
    
    // 模拟流式处理
    for (transaction <- sortedTransactions if running.get()) {
      // 计算时间差（按比例缩放）
      val timeDiff = (transaction.timestamp - prevTimestamp) / timeScaleFactor
      if (timeDiff > 0) {
        Thread.sleep(timeDiff.toLong)
      }
      
      // 获取令牌（限流）
      rateLimiter.acquire()
      
      // 发送到Kafka
      val json = gson.toJson(transaction)
      producer.send(new ProducerRecord[String, String](
        kafkaTopic, transaction.id, json))
      
      prevTimestamp = transaction.timestamp
    }
    
    producer.flush()
    logger.info("交易流模拟完成")
  }
  
  /**
   * 从CSV加载交易数据
   */
  private def loadTransactions(): Seq[Transaction] = {
    val transactions = new ArrayBuffer[Transaction]()
    val reader = new BufferedReader(new FileReader(csvFilePath))
    
    try {
      // 读取表头
      val header = reader.readLine()
      
      // 逐行读取交易
      var line: String = null
      while ({line = reader.readLine(); line != null}) {
        try {
          val fields = line.split("\\|")
          if (fields.length >= 9) {
            val transaction = Transaction(
              id = fields(0),
              cardNum = fields(1),
              timestamp = dateFormat.parse(fields(2)).getTime,
              category = fields(3),
              amount = fields(4).toDouble,
              merchant = normalizeMerchantName(fields(5)),
              merchantLat = if (fields(6).isEmpty) 0.0 else fields(6).toDouble,
              merchantLong = if (fields(7).isEmpty) 0.0 else fields(7).toDouble,
              isFraud = fields(8) == "1"
            )
            transactions += transaction
          }
        } catch {
          case e: Exception => 
            logger.warn(s"解析交易记录失败: $line", e)
        }
      }
    } finally {
      reader.close()
    }
    
    transactions
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
   * 标准化商户名称
   */
  private def normalizeMerchantName(name: String): String = {
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
 * 交易数据结构 - 修改以匹配数据集
 */
case class Transaction(
  id: String,
  cardNum: String,
  timestamp: Long,
  category: String,
  amount: Double,
  merchant: String,
  merchantLat: Double = 0.0,
  merchantLong: Double = 0.0,
  isFraud: Boolean = false,
  description: String = ""  // 添加描述字段
)