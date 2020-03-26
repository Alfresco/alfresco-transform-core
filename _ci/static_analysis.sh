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
     alfresco-transform-pdf-renderer/alfresco-transform-pdf-renderer/target/alfresco-transfrom-pdf-renderer-*.jar \
     alfresco-transform-pdf-renderer/alfresco-transform-pdf-renderer-boot/target/alfresco-transfrom-pdf-renderer-*.jar \
     alfresco-transform-imagemagick/alfresco-transform-imagemagick/target/alfresco-transform-imagemagick-*.jar \
     alfresco-transform-imagemagick/alfresco-transform-imagemagick-spring/target/alfresco-transform-imagemagick-spring-*.jar \
     alfresco-transform-libreoffice/alfresco-transform-libreoffice/target/alfresco-transform-libreoffice-*.jar \
     alfresco-transform-libreoffice/alfresco-transform-libreoffice-spring/target/alfresco-transform-libreoffice-spring-*.jar \
     alfresco-transform-tika/alfresco-transform-tika/target/alfresco-transform-tika-*.jar \
     alfresco-transform-tika/alfresco-transform-tika-spring/target/alfresco-transform-tika-spring-*.jar \
     alfresco-transform-misc/alfresco-transform-misc/target/alfresco-transform-misc-*.jar \
     alfresco-transform-misc/alfresco-transform-misc-spring/target/alfresco-transform-misc-spring*.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

popd
set +vex
echo "=========================== Finishing Static Analysis Script =========================="