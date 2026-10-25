#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
FF_SERVER_JAR="$HYTALE_SERVER_JAR" FF_JAVA_HOME="$JAVA_HOME" bash tools/build.sh
