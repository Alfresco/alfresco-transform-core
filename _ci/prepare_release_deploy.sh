#!/usr/bin/env bash

echo "========================== Starting Prepare Release Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Identify latest annotated tag (latest version)
export VERSION=$(git describe --abbrev=0 --tags)

mkdir -p deploy_dir

# Download the WhiteSource report
mvn -B org.alfresco:whitesource-downloader-plugin:inventoryReport \
    -N \
    "-Dorg.whitesource.product=Transform Service" \
    -DsaveReportAs=deploy_dir/3rd-party.xlsx

echo "Local deploy directory content:"
ls -lA deploy_dir

popd
set +vex
echo "========================== Finishing Prepare Release Deploy Script =========================="
