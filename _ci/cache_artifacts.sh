#!/usr/bin/env bash

echo "=========================== Starting Cache Artifacts Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

LIBREOFFICE_VERSION=26.2.3

# Cache the LibreOffice x86_64 distribution, as it takes a long time to download and can cause the
# build to fail (no output for more than 10 minutes). aarch64 is fetched the same way at build time.
LIBREOFFICE_RPM_URL="https://download.documentfoundation.org/libreoffice/stable/${LIBREOFFICE_VERSION}/rpm/x86_64/LibreOffice_${LIBREOFFICE_VERSION}_Linux_x86-64_rpm.tar.gz"
CACHED_FILE="${HOME}/artifacts/libreoffice-dist-${LIBREOFFICE_VERSION}-linux.tar.gz"
if [ -f "${CACHED_FILE}" ]; then
    echo "Using cached LibreOffice distribution..."
else
    echo "Downloading LibreOffice distribution..."
    curl -s -S "${LIBREOFFICE_RPM_URL}" -o "${CACHED_FILE}" --create-dirs
fi


popd
set +vex
echo "=========================== Finishing Cache Artifacts Script =========================="
