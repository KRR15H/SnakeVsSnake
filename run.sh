#!/bin/sh
# Compiles and runs Snake Vs Snake (Linux / macOS). Needs a JDK (javac) 8 or newer.
cd "$(dirname "$0")" || exit 1
mkdir -p out
find src -name "*.java" > sources.txt
javac -d out -cp "lib/*" @sources.txt || { echo "Compile failed"; rm -f sources.txt; exit 1; }
rm -f sources.txt
java -cp "out:lib/*" snakevssnake.Main
