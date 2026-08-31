#!/usr/bin/env bash
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -x "/usr/lib/jvm/java-25-openjdk/bin/javac" ]; then
  JAVAC="/usr/lib/jvm/java-25-openjdk/bin/javac"
  JAVA="/usr/lib/jvm/java-25-openjdk/bin/java"
else
  JAVAC="/usr/lib/jvm/java-26-openjdk/bin/javac"
  JAVA="/usr/lib/jvm/java-26-openjdk/bin/java"
fi
rm -rf build/test-classes
mkdir -p build/classes build/test-classes
find src -name '*.java' > build/production-sources.txt
find test -name '*.java' > build/test-sources.txt
$JAVAC --release 25 -encoding UTF-8 -Xlint:-options -d build/classes @build/production-sources.txt
$JAVAC --release 25 -encoding UTF-8 -Xlint:-options -cp build/classes -d build/test-classes @build/test-sources.txt
$JAVA -cp "build/classes:build/test-classes" com.classdiagrammer.tests.TestSuites
