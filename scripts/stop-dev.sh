#!/bin/bash
# 停止开发环境

# 配置变量
KAFKA_VERSION=2.13-2.8.1
FLINK_VERSION=1.12.2

# 1. 停止Flink
echo "停止Flink集群..."
cd flink-${FLINK_VERSION}
bin/stop-cluster.sh

# 2. 停止Kafka
echo "停止Kafka..."
cd ../kafka_${KAFKA_VERSION}
bin/kafka-server-stop.sh

# 3. 停止Zookeeper
echo "停止ZooKeeper..."
bin/zookeeper-server-stop.sh

# 4. 停止Redis
echo "停止Redis..."
sudo systemctl stop redis-server

echo "开发环境已停止"