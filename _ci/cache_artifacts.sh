#!/usr/bin/env bash

echo "=========================== Starting Cache Artifacts Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Cache the LibreOffice distribution, as it is takes a long time to download and it can cause the
# build to fail (no output for more than 10 minutes)
LIBREOFFICE_RPM_URL="https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/libreoffice/libreoffice-dist/5.4.6/libreoffice-dist-5.4.6-linux.gz"
if [ -f "${HOME}/artifacts/libreoffice-dist-5.4.6-linux.gz" ]; then
    echo "Using cached LibreOffice distribution..."
else
    echo "Downloading LibreOffice distribution..."
    curl -s -S ${LIBREOFFICE_RPM_URL} -o "${HOME}/artifacts/libreoffice-dist-5.4.6-linux.gz"
fi
cp "${HOME}/artifacts/libreoffice-dist-5.4.6-linux.gz" alfresco-docker-libreoffice/


popd
set +vex
echo "=========================== Finishing Cache Artifacts Script =========================="
