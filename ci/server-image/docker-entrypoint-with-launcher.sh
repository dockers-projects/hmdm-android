#!/bin/sh
set -eu

if [ "${HMDM_VARIANT:-os}" != "os" ]; then
  echo "Bundled launcher image supports HMDM_VARIANT=os only; got: ${HMDM_VARIANT:-<unset>}" >&2
  exit 2
fi

if [ -z "${BUNDLED_LAUNCHER_VERSION:-}" ]; then
  echo "BUNDLED_LAUNCHER_VERSION is not set" >&2
  exit 2
fi

TARGET_DIR=/usr/local/tomcat/work/files
TARGET="${TARGET_DIR}/hmdm-${BUNDLED_LAUNCHER_VERSION}-os.apk"
SOURCE=/opt/hmdm/bundled-launcher/launcher.apk
INIT_SQL=/usr/local/tomcat/work/init.sql

mkdir -p "${TARGET_DIR}"

if [ ! -f "${TARGET}" ] || ! cmp -s "${SOURCE}" "${TARGET}"; then
  cp "${SOURCE}" "${TARGET}"
  chmod 0644 "${TARGET}"
  echo "Seeded bundled launcher APK: ${TARGET}"
else
  echo "Bundled launcher APK already present: ${TARGET}"
fi

# Fresh install: make the stock Headwind bootstrap use the APK version embedded
# in this image instead of downloading another CLIENT_VERSION from h-mdm.com.
#
# Existing install: keep CLIENT_VERSION exactly as supplied by the existing
# compose/.env. The wrapper intentionally does not alter existing DB/config
# selection; the new application version is registered through the web UI.
if [ ! -f "${INIT_SQL}" ]; then
  if [ "${CLIENT_VERSION:-}" != "${BUNDLED_LAUNCHER_VERSION}" ]; then
    echo "Fresh install: using bundled launcher version ${BUNDLED_LAUNCHER_VERSION}"
  fi
  export CLIENT_VERSION="${BUNDLED_LAUNCHER_VERSION}"
else
  echo "Existing installation detected; preserving CLIENT_VERSION=${CLIENT_VERSION:-<unset>}"
fi

# Important: this wrapper intentionally does not mutate the Headwind MDM DB.
exec /docker-entrypoint.sh "$@"
