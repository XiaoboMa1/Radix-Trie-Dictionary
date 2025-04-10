@echo off

cmake -S . -B build
cmake --build build --config Release
.\build\Release\financial.exe

if "%1"=="del" (
    del build\CMakeCache.txt
)

cd build
cmake ..