FROM debian:bullseye-slim

# 安装编译环境和librdkafka
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    git \
    librdkafka-dev \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 创建必要的目录结构
RUN mkdir -p /app/build /app/data

# 默认命令
CMD ["bash"]