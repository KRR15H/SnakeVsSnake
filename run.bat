@echo off
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out -cp "lib/*" @sources.txt
if errorlevel 1 (
  echo Compile failed
  del sources.txt
  pause
  exit /b 1
)
del sources.txt
java -cp "out;lib/*" snakevssnake.Main
