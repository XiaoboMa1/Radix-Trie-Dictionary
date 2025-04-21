package ma.fin.monitor.common.config

/**
 * kafka配置类
 * @param brokers
 * @param groupId
 * @param mode
 * @param timestamp
 */
case class KafkaConfig(brokers: String, groupId: String, mode: String, timestamp: String) extends Serializable {
}
