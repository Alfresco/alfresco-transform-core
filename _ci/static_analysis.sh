#!/usr/bin/env bash

echo "=========================== Starting Static Analysis Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Run in a sandbox for every branch, run normally on master
[ "${TRAVIS_BRANCH}" != "master" ] && RUN_IN_SANDBOX="-sandboxname Transformers" || RUN_IN_SANDBOX=""

java -jar vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar -vid $VERACODE_API_ID \
     -vkey $VERACODE_API_KEY -action uploadandscan -appname "Transform Service" \
     ${RUN_IN_SANDBOX} -createprofile false \
     -filepath \
     alfresco-transform-pdf-renderer/alfresco-transform-pdf-renderer-boot/target/alfresco-transform-pdf-renderer-*.jar \
     alfresco-transform-imagemagick/alfresco-transform-imagemagick-boot/target/alfresco-transform-imagemagick-boot-*.jar \
     alfresco-transform-libreoffice/alfresco-transform-libreoffice-boot/target/alfresco-transform-libreoffice-boot-*.jar \
     alfresco-transform-tika/alfresco-transform-tika-boot/target/alfresco-transform-tika-boot-*.jar \
     alfresco-transform-misc/alfresco-transform-misc-boot/target/alfresco-transform-misc-boot*.jar \
     alfresco-transform-core-aio/alfresco-transform-core-aio-boot/target/alfresco-transform-core-aio-boot*.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

popd
set +vex
echo "=========================== Finishing Static Analysis Script =========================="
