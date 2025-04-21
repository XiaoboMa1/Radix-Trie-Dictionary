package ma.fin.monitor.common.utils.radix

/**
 * RadixTree JNI接口类
 * 提供Scala代码访问C++ RadixTree的桥梁
 */
class RadixTreeJNI {
  // 加载本地库
  System.loadLibrary("radix_tree_jni")
  
  /**
   * 初始化RadixTree
   * 
   * @param dictionaryPath 词典文件路径
   * @return 树句柄
   */
  @native def initialize(dictionaryPath: String): Long
  
  /**
   * 在文本中查找实体
   * 
   * @param handle RadixTree句柄
   * @param text 要搜索的文本
   * @return 找到的实体匹配数组
   */
  @native def findEntities(handle: Long, text: String): Array[EntityMatch]
  
  /**
   * 添加实体到RadixTree
   * 
   * @param handle RadixTree句柄
   * @param entity 实体名称
   * @param entityType 实体类型代码
   * @return 是否添加成功
   */
  @native def addEntity(handle: Long, entity: String, entityType: Int): Boolean
  
  /**
   * 释放RadixTree资源
   * 
   * @param handle RadixTree句柄
   */
  @native def dispose(handle: Long): Unit
}