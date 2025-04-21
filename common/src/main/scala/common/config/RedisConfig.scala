package ma.fin.monitor.common.config

case class RedisConfig(host: String, port: Int, password: String, db: Int) extends Serializable {
}
