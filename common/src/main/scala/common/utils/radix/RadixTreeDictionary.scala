package ma.fin.monitor.common.utils.radix

import org.apache.flink.api.common.functions.{IterationRuntimeContext, RichFunction, RuntimeContext}
import org.apache.flink.configuration.Configuration
import org.slf4j.LoggerFactory

import java.io.{BufferedReader, BufferedWriter, FileReader, FileWriter}
import java.nio.file.{Files, Paths}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

/**
 * RadixTree字典管理类
 * 负责加载、更新和维护RadixTree词典
 */
class RadixTreeDictionary extends RichFunction {
  // 删除重复定义的logger
  private val logger = LoggerFactory.getLogger(getClass)
  
  @transient private var radixTreeJNI: RadixTreeJNI = _
  @transient private var treeHandle: Long = _
  @transient private var refreshExecutor: ScheduledExecutorService = _
  
  // 配置
  private var dictionaryPath: String = _
  private var refreshIntervalMinutes: Int = 60
  
  // RuntimeContext相关字段和方法
  @transient private var runtimeContext: RuntimeContext = _
  
  override def getRuntimeContext(): RuntimeContext = runtimeContext
  
  override def setRuntimeContext(context: RuntimeContext): Unit = {
    this.runtimeContext = context
  }
  
  override def getIterationRuntimeContext(): IterationRuntimeContext = {
    runtimeContext match {
      case iterCtx: IterationRuntimeContext => iterCtx
      case _ => throw new UnsupportedOperationException("当前RuntimeContext不是IterationRuntimeContext")
    }
  }


  
  
  /**
   * 初始化RadixTree
   */
  override def open(parameters: Configuration): Unit = {
    // 从参数获取配置
    dictionaryPath = parameters.getString("dictionary.path", "sanctions_dictionary.txt")
    refreshIntervalMinutes = parameters.getInteger("dictionary.refresh.interval.minutes", 60)
    
    // 从分布式缓存加载词典文件
    val dictFile = getRuntimeContext.getDistributedCache
      .getFile(dictionaryPath)
      
    logger.info(s"初始化RadixTree字典，路径: ${dictFile.getAbsolutePath}")
    
    // 初始化RadixTree
    radixTreeJNI = new RadixTreeJNI()
    treeHandle = radixTreeJNI.initialize(dictFile.getAbsolutePath)
    
    // 启动定时刷新任务
    if (refreshIntervalMinutes > 0) {
      startDictionaryRefreshTimer()
    }
  }
  
  /**
   * 查找文本中的实体
   */
  def findEntities(text: String): Array[EntityMatch] = {
    if (treeHandle == 0) {
      logger.warn("RadixTree未初始化")
      return Array.empty
    }
    
    try {
      radixTreeJNI.findEntities(treeHandle, text)
    } catch {
      case e: Exception =>
        logger.error(s"查找实体出错: ${e.getMessage}", e)
        Array.empty
    }
  }
  
  /**
   * 启动词典刷新定时器
   */
  private def startDictionaryRefreshTimer(): Unit = {
    refreshExecutor = Executors.newSingleThreadScheduledExecutor()
    refreshExecutor.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = refreshDictionary()
      },
      refreshIntervalMinutes,
      refreshIntervalMinutes,
      TimeUnit.MINUTES
    )
    
    logger.info(s"已启动RadixTree字典刷新定时器，间隔: $refreshIntervalMinutes 分钟")
  }
  
  /**
   * 刷新词典数据
   */
  private def refreshDictionary(): Unit = {
    try {
      logger.info("开始刷新RadixTree词典")
      
      // 检查词典文件是否有更新
      val dictFile = getRuntimeContext.getDistributedCache
        .getFile(dictionaryPath)
      
      if (Files.exists(Paths.get(dictFile.getAbsolutePath))) {
        // 关闭旧的RadixTree
        if (treeHandle != 0) {
          radixTreeJNI.dispose(treeHandle)
        }
        
        // 初始化新的RadixTree
        treeHandle = radixTreeJNI.initialize(dictFile.getAbsolutePath)
        logger.info("RadixTree词典刷新完成")
      } else {
        logger.warn(s"词典文件不存在: ${dictFile.getAbsolutePath}")
      }
    } catch {
      case e: Exception =>
        logger.error(s"刷新RadixTree词典出错: ${e.getMessage}", e)
    }
  }
  
  /**
   * 添加实体到词典
   */
  def addEntity(entity: String, entityType: String, save: Boolean = false): Boolean = {
    try {
      val result = radixTreeJNI.addEntity(treeHandle, entity, EntityTypeMapper.getTypeCode(entityType))
      
      // 如果需要，保存到词典文件
      if (save && result) {
        val dictFile = getRuntimeContext.getDistributedCache
          .getFile(dictionaryPath)
        
        val writer = new BufferedWriter(new FileWriter(dictFile, true))
        writer.write(s"$entity\t$entityType\n")
        writer.close()
      }
      
      result
    } catch {
      case e: Exception =>
        logger.error(s"添加实体出错: ${e.getMessage}", e)
        false
    }
  }
  
  /**
   * 释放资源
   */
  override def close(): Unit = {
    if (refreshExecutor != null) {
      refreshExecutor.shutdown()
    }
    
    if (treeHandle != 0) {
      radixTreeJNI.dispose(treeHandle)
    }
  }
  
  /**
   * 从CSV文件加载词典
   */
  def loadDictionaryFromCSV(csvFile: String, entityColumn: Int, typeColumn: Int): Int = {
    var count = 0
    val reader = new BufferedReader(new FileReader(csvFile))
    
    try {
      // 跳过表头
      reader.readLine()
      
      var line: String = null
      while ({line = reader.readLine(); line != null}) {
        val fields = line.split(",", -1)
        if (fields.length > Math.max(entityColumn, typeColumn)) {
          val entity = fields(entityColumn).trim
          val entityType = fields(typeColumn).trim
          
          if (entity.nonEmpty && entityType.nonEmpty) {
            if (addEntity(entity, entityType, false)) {
              count += 1
            }
          }
        }
      }
      
      logger.info(s"从CSV加载了 $count 个实体到RadixTree")
    } finally {
      reader.close()
    }
    
    count
  }
}