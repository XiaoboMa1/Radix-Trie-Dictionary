package ma.fin.monitor.collections.stream

import org.apache.commons.cli.{DefaultParser, Options}
/**
 * 投诉数据流模拟器启动器
 */
object ComplaintStreamSimulatorRunner {
  def main(args: Array[String]): Unit = {
    // 解析命令行参数
    val options = new Options()
    options.addOption("i", "input", true, "输入CSV文件路径")
    options.addOption("t", "topic", true, "Kafka主题名")
    options.addOption("r", "rate", true, "每秒事件数")
    options.addOption("s", "scale", true, "时间缩放因子")
    
    val parser = new DefaultParser()
    val cmd = parser.parse(options, args)
    
    val inputFile = cmd.getOptionValue("input", "data/complaints/complaints.csv")
    val topic = cmd.getOptionValue("topic", "financial-complaints")
    val rate = cmd.getOptionValue("rate", "5.0").toDouble
    val scale = cmd.getOptionValue("scale", "86400.0").toDouble
    
    println(s"启动投诉流模拟器：输入=$inputFile, 主题=$topic, 速率=$rate/秒, 缩放=$scale")
    
    // 创建并启动模拟器
    val simulator = new ComplaintStreamSimulator(inputFile, topic, rate, scale)
    simulator.start()
    
    // 注册关闭钩子
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      println("正在关闭投诉流模拟器...")
      simulator.stop()
    }))
    
    // 等待用户输入以停止
    println("按任意键停止模拟器...")
    System.in.read()
    simulator.stop()
  }
}