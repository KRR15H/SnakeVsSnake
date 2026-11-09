@echo off
cd /d "%~dp0"
if not exist out mkdir out
if not exist test-out mkdir test-out
dir /s /b src\*.java > sources.txt
javac -d out -cp "lib/*" @sources.txt
del sources.txt
dir /s /b test\*.java > tests.txt
javac -cp "out;lib/*" -d test-out @tests.txt
del tests.txt
java -Djava.awt.headless=true -cp "out;test-out;lib/*" snakevssnake.GameTest
pause
