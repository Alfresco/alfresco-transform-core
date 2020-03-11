#!/usr/bin/env bash

echo "=========================== Starting Static Analysis Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

java -jar vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar -vid $VERACODE_API_ID \
     -vkey $VERACODE_API_KEY -action uploadandscan -appname "Transform Service" \
     -sandboxname "Transformer Base Sandbox" \
     -createprofile false -filepath alfresco-transformer-base/target/alfresco-transformer-base-2.1.2-SNAPSHOT.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

java -jar vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar -vid $VERACODE_API_ID \
     -vkey $VERACODE_API_KEY -action uploadandscan -appname "Transform Service" \
     -sandboxname "Pdf Renderer Sandbox" \
     -createprofile false -filepath alfresco-docker-alfresco-pdf-renderer/target/alfresco-docker-alfresco-pdf-renderer-2.1.2-SNAPSHOT.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

java -jar vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar -vid $VERACODE_API_ID \
     -vkey $VERACODE_API_KEY -action uploadandscan -appname "Transform Service" \
     -sandboxname "Imagemagick Sandbox" \
     -createprofile false -filepath alfresco-docker-imagemagick/target/alfresco-docker-imagemagick-2.1.2-SNAPSHOT.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

java -jar vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar -vid $VERACODE_API_ID \
     -vkey $VERACODE_API_KEY -action uploadandscan -appname "Transform Service" \
     -sandboxname "Libreoffice Sandbox" \
     -createprofile false -filepath alfresco-docker-libreoffice/target/alfresco-docker-libreoffice-2.1.2-SNAPSHOT.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

java -jar vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar -vid $VERACODE_API_ID \
     -vkey $VERACODE_API_KEY -action uploadandscan -appname "Transform Service" \
     -sandboxname "Tika Sandbox" \
     -createprofile false -filepath alfresco-docker-tika/target/alfresco-docker-tika-2.1.2-SNAPSHOT.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

java -jar vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar -vid $VERACODE_API_ID \
     -vkey $VERACODE_API_KEY -action uploadandscan -appname "Transform Service" \
     -sandboxname "Misc Sandbox" \
     -createprofile false -filepath alfresco-docker-transform-misc/target/alfresco-docker-transform-misc-2.1.2-SNAPSHOT.jar \
     -version "$TRAVIS_JOB_ID - $TRAVIS_JOB_NUMBER" -scantimeout 3600

popd
set +vex
echo "=========================== Finishing Static Analysis Script =========================="