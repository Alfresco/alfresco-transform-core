#!/usr/bin/env bash

echo "=========================== Starting Static Analysis Init Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

wget https://repo1.maven.org/maven2/com/veracode/vosp/api/wrappers/vosp-api-wrappers-java/$VERACODE_WRAPPER_VERSION/vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar
sha1sum -c <<< "$VERACODE_WRAPPER_SHA1 vosp-api-wrappers-java-$VERACODE_WRAPPER_VERSION.jar"

popd
set +vex
echo "=========================== Finishing Static Analysis Init Script =========================="
