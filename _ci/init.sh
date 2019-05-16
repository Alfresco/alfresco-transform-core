#!/usr/bin/env bash

echo "=========================== Starting Init Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

mkdir -p ${HOME}/.m2 && cp -rf _ci/settings.xml ${HOME}/.m2/
echo "${QUAY_PASSWORD}" | docker login -u="alfresco+bamboo" --password-stdin quay.io
find "${HOME}/.m2/repository/" -type d -name "*-SNAPSHOT*" | xargs -r -l rm -rf

# Cache the LibreOffice distribution, as it is takes a long time to download and it can cause the
# build to fail (no output for more than 10 minutes)
LIBREOFFICE_RPM_URL="https://nexus.alfresco.com/nexus/service/local/repositories/thirdparty/content/org/libreoffice/libreoffice-dist/5.4.6/libreoffice-dist-5.4.6-linux.gz"
if [ ! -f "${HOME}/.m2/repository/libreoffice-dist-5.4.6-linux.gz" ]; then
    curl -s -S ${LIBREOFFICE_RPM_URL} -o "${HOME}/.m2/repository/libreoffice-dist-5.4.6-linux.gz"
fi
cp "${HOME}/.m2/repository/libreoffice-dist-5.4.6-linux.gz" alfresco-docker-libreoffice/


popd
set +vex
echo "=========================== Finishing Init Script =========================="

