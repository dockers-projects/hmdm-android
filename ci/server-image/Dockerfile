ARG HMDM_BASE_IMAGE
FROM ${HMDM_BASE_IMAGE}

ARG CLIENT_VERSION
ARG VCS_REF=unknown

ENV BUNDLED_LAUNCHER_VERSION="${CLIENT_VERSION}" \
    CLIENT_VERSION="${CLIENT_VERSION}" \
    HMDM_VARIANT="os"

LABEL org.opencontainers.image.source="https://github.com/dockers-projects/hmdm-android" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.description="Headwind MDM server image with a bundled custom com.hmdm.launcher APK"

COPY dist/launcher.apk /opt/hmdm/bundled-launcher/launcher.apk
COPY ci/server-image/docker-entrypoint-with-launcher.sh /docker-entrypoint-with-launcher.sh

RUN test -s /opt/hmdm/bundled-launcher/launcher.apk \
    && chmod 0755 /docker-entrypoint-with-launcher.sh

ENTRYPOINT ["/docker-entrypoint-with-launcher.sh"]
