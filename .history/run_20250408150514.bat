@echo off
cmake -S . -B build
cmake --build build --config Release
.\build\Release\Financial.exe
