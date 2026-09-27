#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_SDK_ROOT:?ANDROID_SDK_ROOT must be set}"
: "${ANDROID_KEYSTORE_FILE:?ANDROID_KEYSTORE_FILE must point to the release keystore}"
: "${ANDROID_KEYSTORE_PASSWORD:?ANDROID_KEYSTORE_PASSWORD must be set}"
: "${ANDROID_KEY_ALIAS:?ANDROID_KEY_ALIAS must be set}"
: "${ANDROID_KEY_PASSWORD:?ANDROID_KEY_PASSWORD must be set}"

BUILD_TOOLS="${ANDROID_SDK_ROOT}/build-tools/35.0.0"
AAPT="${BUILD_TOOLS}/aapt"
APKSIGNER="${BUILD_TOOLS}/apksigner"
ZIPALIGN="${BUILD_TOOLS}/zipalign"

for tool in "${AAPT}" "${APKSIGNER}" "${ZIPALIGN}"; do
  test -x "${tool}" || { echo "Missing Android build tool: ${tool}" >&2; exit 1; }
done

VERSION_NAME="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle | head -n1)"
VERSION_CODE="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*\([0-9][0-9]*\).*/\1/p' app/build.gradle | head -n1)"

test -n "${VERSION_NAME}" || { echo "Unable to read versionName" >&2; exit 1; }
test -n "${VERSION_CODE}" || { echo "Unable to read versionCode" >&2; exit 1; }

mkdir -p "${HOME}" "${GRADLE_USER_HOME}"
rm -rf dist
mkdir -p dist

echo "Building com.hmdm.launcher ${VERSION_NAME} (${VERSION_CODE})"
bash ./gradlew --no-daemon --console=plain --stacktrace assembleOpensourceRelease

UNSIGNED="$(find app/build/outputs/apk/opensource/release -maxdepth 1 -type f -name '*release*.apk' | head -n1)"
test -n "${UNSIGNED}" && test -s "${UNSIGNED}" || { echo "Unsigned release APK not found" >&2; exit 1; }

ALIGNED="dist/hmdm-${VERSION_NAME}-os-aligned.apk"
SIGNED="dist/hmdm-${VERSION_NAME}-os.apk"

"${ZIPALIGN}" -p -f 4 "${UNSIGNED}" "${ALIGNED}"
"${APKSIGNER}" sign   --ks "${ANDROID_KEYSTORE_FILE}"   --ks-pass env:ANDROID_KEYSTORE_PASSWORD   --ks-key-alias "${ANDROID_KEY_ALIAS}"   --key-pass env:ANDROID_KEY_PASSWORD   --out "${SIGNED}"   "${ALIGNED}"

"${APKSIGNER}" verify --verbose --print-certs "${SIGNED}"

BADGING="$("${AAPT}" dump badging "${SIGNED}" | head -n1)"
echo "${BADGING}"
echo "${BADGING}" | grep -q "package: name='com.hmdm.launcher'"
echo "${BADGING}" | grep -q "versionCode='${VERSION_CODE}'"
echo "${BADGING}" | grep -q "versionName='${VERSION_NAME}'"

"${APKSIGNER}" verify --print-certs "${SIGNED}" > "dist/signing-certificate.txt"
sha256sum "${SIGNED}" > "dist/hmdm-${VERSION_NAME}-os.apk.sha256"

cp "${SIGNED}" dist/launcher.apk
rm -f "${ALIGNED}"

echo "VERSION_NAME=${VERSION_NAME}"
echo "VERSION_CODE=${VERSION_CODE}"
echo "SIGNED_APK=${SIGNED}"
