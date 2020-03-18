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
     alfresco-transformer-base/target/alfresco-transformer-base-*.jar \
     alfresco-docker-alfresco-pdf-renderer/target/alfresco-docker-alfresco-pdf-renderer-*.jar \
     alfresco-docker-imagemagick/target/alfresco-docker-imagemagick-*.jar \
     alfresco-docker-libreoffice/target/alfresco-docker-libreoffice-*.jar \
     alfresco-docker-tika/target/alfresco-docker-tika-*.jar \
     alfresco-docker-transform-misc/target/alfresco-docker-transform-misc-*.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

popd
set +vex
echo "=========================== Finishing Static Analysis Script =========================="