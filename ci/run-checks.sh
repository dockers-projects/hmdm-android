#!/usr/bin/env bash
set -euo pipefail

mkdir -p "${HOME}" "${GRADLE_USER_HOME}"

echo "== Java =="
java -version

echo "== Gradle =="
./gradlew --version

echo "== Gate 1/3: launcher unit tests =="
./gradlew --no-daemon --console=plain --stacktrace testOpensourceDebugUnitTest

echo "== Gate 2/3: Android lint =="
./gradlew --no-daemon --console=plain --stacktrace lintOpensourceDebug

echo "== Gate 3/3: debug APK assembly =="
./gradlew --no-daemon --console=plain --stacktrace assembleOpensourceDebug

APK="app/build/outputs/apk/opensource/debug/app-opensource-debug.apk"
if [[ ! -s "$APK" ]]; then
    echo "Expected APK not found or empty: $APK" >&2
    exit 1
fi

echo "All gates passed."
ls -lh "$APK"
