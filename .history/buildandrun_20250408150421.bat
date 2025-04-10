@echo off
cmake -S . -B build
cmake --build build --config Release
.\build\Release\your_program.exe
