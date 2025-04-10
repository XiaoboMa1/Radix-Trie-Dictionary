@echo off
rmdir /s /q build
mkdir build

echo 配置项目...
cmake -S . -B build -G "MinGW Makefiles"

echo 构建项目...
cmake --build build

echo 运行程序...
build\financialDemo.exe