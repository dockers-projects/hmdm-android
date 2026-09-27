#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 OLD.apk NEW.apk" >&2
  exit 2
fi

: "${ANDROID_SDK_ROOT:?ANDROID_SDK_ROOT must be set}"
BUILD_TOOLS="${ANDROID_SDK_ROOT}/build-tools/35.0.0"
AAPT="${BUILD_TOOLS}/aapt"
APKSIGNER="${BUILD_TOOLS}/apksigner"

OLD="$1"
NEW="$2"

for apk in "${OLD}" "${NEW}"; do
  test -s "${apk}" || { echo "APK not found or empty: ${apk}" >&2; exit 1; }
done

badging() { "${AAPT}" dump badging "$1" | head -n1; }
pkg() { badging "$1" | sed -n "s/.*package: name='\([^']*\)'.*/\1/p"; }
code() { badging "$1" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p"; }
cert() {
  "${APKSIGNER}" verify --print-certs "$1" |
    sed -n 's/^Signer #1 certificate SHA-256 digest: //p' |
    head -n1
}

OLD_PKG="$(pkg "${OLD}")"
NEW_PKG="$(pkg "${NEW}")"
OLD_CODE="$(code "${OLD}")"
NEW_CODE="$(code "${NEW}")"
OLD_CERT="$(cert "${OLD}")"
NEW_CERT="$(cert "${NEW}")"

printf 'old package=%s versionCode=%s cert=%s\n' "${OLD_PKG}" "${OLD_CODE}" "${OLD_CERT}"
printf 'new package=%s versionCode=%s cert=%s\n' "${NEW_PKG}" "${NEW_CODE}" "${NEW_CERT}"

[[ "${OLD_PKG}" == "com.hmdm.launcher" && "${NEW_PKG}" == "com.hmdm.launcher" ]] ||
  { echo "Package mismatch" >&2; exit 1; }

[[ "${OLD_CERT}" == "${NEW_CERT}" ]] ||
  { echo "Signing certificate mismatch: Android will not perform an in-place update" >&2; exit 1; }

(( NEW_CODE > OLD_CODE )) ||
  { echo "New versionCode must be greater than old versionCode" >&2; exit 1; }

echo "Upgrade compatibility check passed."
