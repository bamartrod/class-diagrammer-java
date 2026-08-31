#!/usr/bin/env bash
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

rm -rf build/test-classes
mkdir -p build/classes build/test-classes
find src -name '*.java' > build/production-sources.txt
find test -name '*.java' > build/test-sources.txt

/usr/lib/jvm/java-26-openjdk/bin/javac --release 25 -encoding UTF-8 -Xlint:-options -d build/classes @build/production-sources.txt
/usr/lib/jvm/java-26-openjdk/bin/javac --release 25 -encoding UTF-8 -Xlint:-options -cp build/classes -d build/test-classes @build/test-sources.txt

/usr/lib/jvm/java-26-openjdk/bin/java -cp "build/classes:build/test-classes" com.classdiagrammer.tests.TestSuites
