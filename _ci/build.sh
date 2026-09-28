#!/usr/bin/env bash

echo "=========================== Starting Build Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

mvn -B -U \
    clean install \
    -DadditionalOption=-Xdoclint:none -Dmaven.javadoc.skip=true \
    -DskipTests \
    -Dmagick.backend="${MAGICK_BACKEND:-im}" \
    "-P$1,"

popd
set +vex
echo "=========================== Finishing Build Script =========================="
