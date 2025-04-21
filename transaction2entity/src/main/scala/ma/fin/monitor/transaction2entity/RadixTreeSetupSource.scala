package ma.fin.monitor.transaction2entity

// Correct the import path for utils
import ma.fin.monitor.common.utils.FileUtils // Assuming FileUtils contains readFileLines
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.slf4j.{Logger, LoggerFactory}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.collection.JavaConverters._ // Use ScalaConverters in newer Scala versions if needed

// Import RadixTreeJNI from the native-lib module
import ma.fin.monitor.nativelib.RadixTreeJNI // Adjust package if different

/**
 * Source function to initialize and broadcast the Radix Tree.
 */
class RadixTreeSetupSource(dictPath: String) extends SourceFunction[RadixTreeJNI] {
  private val logger = LoggerFactory.getLogger(getClass)
  private var isRunning = true

  override def run(ctx: SourceFunction.SourceContext[RadixTreeJNI]): Unit = {
    try {
      logger.info(s"Initializing Radix Tree from dictionary: $dictPath")
      val tree = new RadixTreeJNI() // Use imported type

      // Use the correctly imported FileUtils or standard Java IO
      val lines = FileUtils.readFileLines(dictPath) // Or use Files.readAllLines
      // val lines = Files.readAllLines(Paths.get(dictPath), StandardCharsets.UTF_8).asScala

      var count = 0
      lines.foreach { line =>
        val parts = line.split("\\s+", 2) // Assuming format: word [optional data]
        if (parts.nonEmpty) {
          tree.insert(parts(0)) // Assuming insert takes just the word
          count += 1
        }
      }
      logger.info(s"Successfully inserted $count words into the Radix Tree.")
      ctx.collect(tree) // Emit the initialized tree
    } catch {
      case e: Exception =>
        logger.error(s"Failed to initialize Radix Tree from path $dictPath", e)
        // Decide how to handle failure: throw, emit null, etc.
        throw new RuntimeException("Radix Tree initialization failed", e)
    }

    // 保持运行直到作业取消
    while (isRunning) {
      Thread.sleep(5000)
    }
  }

  override def cancel(): Unit = {
    isRunning = false
  }
}

/**
 * 制裁数据处理器
 */
class SanctionsDataProcessor {
  private val logger = LoggerFactory.getLogger(getClass)
  
  def processSanctionsData(outputDictionaryPath: String): Unit = {
    logger.info("处理制裁数据...")
    
    try {
      // 加载三个制裁名单源
      val disqualifiedDirectors = loadCSV("data/sanctions/UK_Companies_House_Disqualified_Directors.csv")
      val fcdoSanctions = loadCSV("data/sanctions/UK_FCDO_Sanctions_List.csv")
      val hmtofsiTargets = loadCSV("data/sanctions/UK_HMTOFSI_Consolidated_List_of_Targets.csv")
      
      // 合并所有实体
      val allEntities = disqualifiedDirectors ++ fcdoSanctions ++ hmtofsiTargets
      
      // 去重
      val uniqueEntities = deduplicateEntities(allEntities)
      
      logger.info(s"制裁名单数量: 总数=${allEntities.size}, 去重后=${uniqueEntities.size}")
      
      // 写入字典文件
      writeToRadixDictionary(uniqueEntities, outputDictionaryPath)
      
      // 初始化RadixTree实例
      initializeRadixTree(outputDictionaryPath)
      
    } catch {
      case e: Exception => 
        logger.error("处理制裁数据时出错", e)
    }
  }
  
  /**
   * 加载CSV文件
   */
  def loadCSV(filePath: String): List[SanctionedEntity] = {
    val entities = ArrayBuffer[SanctionedEntity]()
    val reader = new BufferedReader(new FileReader(filePath))
    
    try {
      // 跳过表头
      val header = reader.readLine()
      val columns = parseCSVLine(header)
      
      // 解析每行
      var line: String = null
      while ({line = reader.readLine(); line != null}) {
        try {
          val fields = parseCSVLine(line)
          
          // 根据不同制裁名单的格式处理
          if (filePath.contains("Disqualified_Directors")) {
            // 处理UK Companies House格式
            val nameIdx = columns.indexOf("NAME")
            val idIdx = columns.indexOf("DIRECTOR_ID")
            
            if (nameIdx >= 0 && idIdx >= 0 && fields.length > Math.max(nameIdx, idIdx)) {
              entities += SanctionedEntity(
                id = s"UKCH-${fields(idIdx)}",
                name = fields(nameIdx),
                entityType = "INDIVIDUAL",
                aliases = List(),
                birthDate = "",
                countries = List("GB"),
                sanctionDetail = "UK Companies House Disqualified Director",
                sourceSystem = "UKCH",
                firstSeen = System.currentTimeMillis()
              )
            }
          } else if (filePath.contains("FCDO")) {
            // 处理UK FCDO格式
            val nameIdx = columns.indexOf("NAME")
            val idIdx = columns.indexOf("ID")
            val typeIdx = columns.indexOf("ENTITY_TYPE") 
            
            if (nameIdx >= 0 && fields.length > nameIdx) {
              entities += SanctionedEntity(
                id = if (idIdx >= 0 && fields.length > idIdx) s"FCDO-${fields(idIdx)}" else s"FCDO-${entities.size}",
                name = fields(nameIdx),
                entityType = if (typeIdx >= 0 && fields.length > typeIdx) mapEntityType(fields(typeIdx)) else "GENERIC",
                aliases = List(),
                birthDate = "",
                countries = List(),
                sanctionDetail = "UK FCDO Sanctions List",
                sourceSystem = "FCDO",
                firstSeen = System.currentTimeMillis()
              )
            }
          } else if (filePath.contains("HMTOFSI")) {
            // 处理UK HMTOFSI格式
            val nameIdx = columns.indexOf("NAME")
            val idIdx = columns.indexOf("ID")
            val typeIdx = columns.indexOf("ENTITY_TYPE")
            
            if (nameIdx >= 0 && fields.length > nameIdx) {
              entities += SanctionedEntity(
                id = if (idIdx >= 0 && fields.length > idIdx) s"HMTOFSI-${fields(idIdx)}" else s"HMTOFSI-${entities.size}",
                name = fields(nameIdx),
                entityType = if (typeIdx >= 0 && fields.length > typeIdx) mapEntityType(fields(typeIdx)) else "GENERIC",
                aliases = List(),
                birthDate = "",
                countries = List(),
                sanctionDetail = "UK HMTOFSI Consolidated List",
                sourceSystem = "HMTOFSI",
                firstSeen = System.currentTimeMillis()
              )
            }
          }
        } catch {
          case e: Exception =>
            logger.warn(s"解析行失败: $line", e)
        }
      }
    } finally {
      reader.close()
    }
    
    entities.toList
  }
  
  /**
   * 解析CSV行，处理引号内的逗号
   */
  private def parseCSVLine(line: String): Array[String] = {
    val tokens = ArrayBuffer[String]()
    var inQuotes = false
    val currentToken = new StringBuilder
    
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
   * 将来源系统的实体类型映射到RadixTree的实体类型
   */
  private def mapEntityType(sourceType: String): String = {
    sourceType.toUpperCase match {
      case t if t.contains("INDIVIDUAL") || t.contains("PERSON") => "INDIVIDUAL"
      case t if t.contains("BANK") || t.contains("FINANCIAL") => "FINANCIAL_INSTITUTION"
      case t if t.contains("COMPANY") || t.contains("CORPORATION") => "CORPORATE_ENTITY"
      case t if t.contains("GOVERNMENT") || t.contains("MINISTRY") => "GOVERNMENT_BODY"
      case t if t.contains("VESSEL") || t.contains("SHIP") => "VESSEL"
      case t if t.contains("AIRCRAFT") => "AIRCRAFT"
      case t if t.contains("PEP") || t.contains("POLITICAL") => "PEP"
      case _ => "SANCTIONED_ENTITY"
    }
  }
  
  /**
   * 实体去重
   */
  def deduplicateEntities(entities: List[SanctionedEntity]): List[SanctionedEntity] = {
    // 按名称分组并保留第一个出现的实体
    val entityMap = new HashMap[String, SanctionedEntity]()
    
    for (entity <- entities) {
      val normalizedName = normalizeEntityName(entity.name)
      if (!entityMap.contains(normalizedName)) {
        entityMap(normalizedName) = entity
      }
    }
    
    entityMap.values.toList
  }
  
  /**
   * 标准化实体名称以便去重
   */
  private def normalizeEntityName(name: String): String = {
    name.trim
      .toUpperCase
      .replaceAll("\\s+", " ")
      .replaceAll("[^A-Z0-9 ]", "")
      .replaceAll("\\bLTD\\b", "LIMITED")
      .replaceAll("\\bCORP\\b", "CORPORATION")
      .replaceAll("\\bINC\\b", "INCORPORATED")
      .replaceAll("\\bPLC\\b", "PUBLIC LIMITED COMPANY")
  }
  
  /**
   * 写入RadixTree字典
   */
  def writeToRadixDictionary(entities: List[SanctionedEntity], outputPath: String): Unit = {
    logger.info(s"写入RadixTree字典: $outputPath, 实体数量: ${entities.size}")
    
    val lines = entities.map { entity =>
      // 格式: 实体名称|实体类型|实体ID
      s"${entity.name}|${entity.entityType}|${entity.id}"
    }
    
    Files.write(
      Paths.get(outputPath),
      lines.mkString("\n").getBytes(StandardCharsets.UTF_8)
    )
    
    logger.info(s"RadixTree字典已写入: $outputPath")
  }
  
  /**
   * 初始化RadixTree
   */
  private def initializeRadixTree(dictionaryPath: String): Unit = {
    try {
      logger.info(s"初始化RadixTree实例: $dictionaryPath")
      
      val radixTree = new RadixTreeJNI()
      val handle = radixTree.initialize(dictionaryPath)
      
      // 此处不关闭，因为它会在全局可用
      logger.info(s"RadixTree初始化成功，实体数量: ${handle}")
    } catch {
      case e: Exception =>
        logger.error("初始化RadixTree失败", e)
    }
  }
}

/**
 * 制裁实体数据结构
 */
case class SanctionedEntity(
  id: String,
  name: String,
  entityType: String,
  aliases: List[String],
  birthDate: String,
  countries: List[String],
  sanctionDetail: String,
  sourceSystem: String,
  firstSeen: Long
)