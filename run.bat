@echo off
rmdir /s /q build
mkdir build
cmake -S . -B build -G "MinGW Makefiles"

echo build...
cmake --build build

echo 运行程序...
build\financialDemo.exe