cmake_minimum_required(VERSION 3.10)
project(RadixTreeKafka)

set(CMAKE_CXX_STANDARD 17)

# 包含目录
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# 设置librdkafka路径
if(WIN32)
    # Windows本地编译配置
    set(RDKAFKA_INCLUDE_DIR "D:/Programming/librdkafka/include")
    set(RDKAFKA_LIB_DIR "D:/Programming/librdkafka/lib")
    include_directories(${RDKAFKA_INCLUDE_DIR})
    link_directories(${RDKAFKA_LIB_DIR})
else()
    # Docker/Linux环境配置
    include_directories(/usr/local/include)
    link_directories(/usr/local/lib)
endif()

# 生成RadixTree库
add_library(radix_tree STATIC 
    ${CMAKE_CURRENT_SOURCE_DIR}/src/radixTree.cpp
)

# 生成Kafka生产者可执行文件
add_executable(transaction_producer
    ${CMAKE_CURRENT_SOURCE_DIR}/src/TransactionProducer.cpp
)
target_link_libraries(transaction_producer
    radix_tree
    rdkafka++
    rdkafka
)

# 生成Kafka消费者可执行文件 
add_executable(transaction_consumer
    ${CMAKE_CURRENT_SOURCE_DIR}/src/TransactionConsumer.cpp
)
target_link_libraries(transaction_consumer
    radix_tree
    rdkafka++
    rdkafka
)