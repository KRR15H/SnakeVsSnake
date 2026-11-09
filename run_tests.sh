#!/bin/sh
# Compiles and runs the automated tests (no window needed).
cd "$(dirname "$0")" || exit 1
mkdir -p out test-out
find src -name "*.java" > sources.txt; javac -d out -cp "lib/*" @sources.txt; rm -f sources.txt
find test -name "*.java" > tests.txt; javac -cp "out:lib/*" -d test-out @tests.txt; rm -f tests.txt
java -Djava.awt.headless=true -cp "out:test-out:lib/*" snakevssnake.GameTest
