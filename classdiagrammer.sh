#!/usr/bin/env bash
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Select JDK 25 if available, fallback to 26 for cross-compilation
if [ -x "/usr/lib/jvm/java-25-openjdk/bin/javac" ]; then
  JAVAC="/usr/lib/jvm/java-25-openjdk/bin/javac"
  JAVA="/usr/lib/jvm/java-25-openjdk/bin/java"
else
  JAVAC="/usr/lib/jvm/java-26-openjdk/bin/javac"
  JAVA="/usr/lib/jvm/java-26-openjdk/bin/java"
fi
rm -rf build/classes
mkdir -p build/classes
find src -name '*.java' > build/production-sources.txt
$JAVAC --release 25 -encoding UTF-8 -Xlint:-options -d build/classes @build/production-sources.txt
exec $JAVA -Xss8m ${JAVA_OPTS:-} -cp build/classes com.classdiagrammer.interfaces.cli.Main "$@"
