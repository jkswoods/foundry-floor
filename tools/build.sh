#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
FF_JDK="${FF_JAVA_HOME:-$PWD/../.tools/jdk-25.0.4.1+1/Contents/Home}"
FF_SERVER="${FF_SERVER_JAR:-$PWD/.cache/pins/0.7.3/Server-0.7.3.jar}"
if [[ ! -x "$FF_JDK/bin/javac" || ! -f "$FF_SERVER" ]]; then
  echo 'Set FF_JAVA_HOME to JDK25 and FF_SERVER_JAR to the pinned Hytale0.7.3 server.' >&2
  exit 1
fi
mkdir -p build/classes build/test-classes build/reports build/libs
python3 - <<'PY'
from pathlib import Path
for folder in ['build/classes','build/test-classes']:
    for p in Path(folder).rglob('*'):
        if p.is_file():p.unlink()
PY
find src/main/java/dev/foundryfloor/core -name '*.java' | sort > build/core-sources.txt
"$FF_JDK/bin/javac" --release 25 -Xlint:all -d build/classes @build/core-sources.txt
find src/test/java -name '*.java' | sort > build/test-sources.txt
"$FF_JDK/bin/javac" --release 25 -Xlint:all -cp build/classes -d build/test-classes @build/test-sources.txt
"$FF_JDK/bin/java" -ea -cp build/classes:build/test-classes dev.foundryfloor.core.CoreTests | tee build/reports/core-tests.txt
if [[ "${1:-}" == '--core' ]]; then exit 0; fi
find src/main/java/dev/foundryfloor/hytale -name '*.java' | sort > build/plugin-sources.txt
"$FF_JDK/bin/javac" --release 25 -Xlint:all -cp "build/classes:$FF_SERVER" -d build/classes @build/plugin-sources.txt
find src/test/native -name '*.java' | sort > build/native-test-sources.txt
"$FF_JDK/bin/javac" --release 25 -Xlint:all -cp "build/classes:$FF_SERVER" -d build/test-classes @build/native-test-sources.txt
"$FF_JDK/bin/java" -ea -cp "build/classes:build/test-classes:$FF_SERVER" dev.foundryfloor.hytale.NativeFootprintTests | tee build/reports/native-tests.txt
python3 src/test/assets/architecture_test.py
python3 tools/package_runtime.py
