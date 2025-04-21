#!/bin/bash
# 启动开发环境

# 配置变量
KAFKA_VERSION=2.13-2.8.1
ZOOKEEPER_PORT=2181
KAFKA_PORT=9092
FLINK_VERSION=1.12.2

# 内存配置 - 针对8GB内存机器优化
export KAFKA_HEAP_OPTS="-Xmx512M -Xms512M"
export FLINK_JOBMANAGER_HEAP=512m
export FLINK_TASKMANAGER_HEAP=1024m
export FLINK_TASKMANAGER_SLOTS=2

# 确保目录存在
mkdir -p data/complaints data/transactions data/sanctions

# 1. 启动Zookeeper
echo "启动ZooKeeper..."
cd kafka_${KAFKA_VERSION}
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties

sleep 5

# 2. 启动Kafka
echo "启动Kafka..."
bin/kafka-server-start.sh -daemon config/server.properties

sleep 5

# 3. 创建必要的Kafka主题
echo "创建Kafka主题..."
bin/kafka-topics.sh --create --topic financial-complaints --bootstrap-server localhost:${KAFKA_PORT} --partitions 4 --replication-factor 1 --if-not-exists
bin/kafka-topics.sh --create --topic financial-transactions --bootstrap-server localhost:${KAFKA_PORT} --partitions 4 --replication-factor 1 --if-not-exists
bin/kafka-topics.sh --create --topic entity-mentions --bootstrap-server localhost:${KAFKA_PORT} --partitions 4 --replication-factor 1 --if-not-exists
bin/kafka-topics.sh --create --topic entity-risk-alerts --bootstrap-server localhost:${KAFKA_PORT} --partitions 4 --replication-factor 1 --if-not-exists

# 4. 启动Flink
echo "启动Flink集群..."
cd ../flink-${FLINK_VERSION}
bin/start-cluster.sh

# 5. 启动Redis
echo "启动Redis..."
sudo systemctl start redis-server

echo "开发环境已启动"
echo "访问Flink控制面板: http://localhost:8081"