#!/usr/bin/env bash
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

rm -rf build/classes
mkdir -p build/classes
find src -name '*.java' > build/production-sources.txt
/usr/lib/jvm/java-17-openjdk/bin/javac --release 17 -encoding UTF-8 -Xlint:-options -d build/classes @build/production-sources.txt

exec /usr/lib/jvm/java-17-openjdk/bin/java -Xss8m ${JAVA_OPTS:-} -cp build/classes com.classdiagrammer.interfaces.cli.Main "$@"
