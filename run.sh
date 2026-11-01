#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p bin
find src tests -name '*.java' -print > bin/sources.txt
javac --release 17 -encoding UTF-8 -d bin @bin/sources.txt
case "${1:-demo}" in
    demo) java -cp bin Main ;;
    test) java -cp bin GraphTest ;;
    benchmark) java -cp bin experiments.Benchmark ;;
    all)
        java -cp bin GraphTest
        java -cp bin Main
        java -cp bin experiments.Benchmark
        ;;
    *) echo "Usage: sh run.sh [demo|test|benchmark|all]"; exit 1 ;;
esac
