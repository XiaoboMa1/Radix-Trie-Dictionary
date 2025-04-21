package ma.fin.monitor.common.config

// 使用别名避免冲突
import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory}
// 使用别名避免冲突
import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory}

object Config {
  // 使用修改后的类型名
  private val load: TypesafeConfig = ConfigFactory.load()

  val kafkaBrokers        : String = load.getString("kafka.brokers")
  val hbaseZookeeperQuorum: String = load.getString("hbase.zookeeper.quorum")

  val redisHost: String     = load.getString("redis.host")
  val redisPort: Int        = load.getInt("redis.port")
  val redisPassword: String = load.getString("redis.password")
  val redisDb: Int          = load.getInt("redis.db")

}
