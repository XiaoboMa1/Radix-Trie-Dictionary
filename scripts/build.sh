#!/bin/bash
# 构建整个项目

# 配置变量
PROJECT_DIR=$(pwd)

echo "开始构建项目..."

# 1. 构建C++ RadixTree库
echo "构建RadixTree原生库..."
cd $PROJECT_DIR/native-lib/src/main/cpp
mkdir -p build && cd build
cmake .. && make
if [ $? -ne 0 ]; then
    echo "RadixTree构建失败!"
    exit 1
fi
echo "RadixTree构建完成"

# 2. 构建Java项目
echo "构建Java项目..."
cd $PROJECT_DIR
mvn clean package
if [ $? -ne 0 ]; then
    echo "Maven构建失败!"
    exit 1
fi
echo "Java项目构建完成"

echo "项目构建完成!"