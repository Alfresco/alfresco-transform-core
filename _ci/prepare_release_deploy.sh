#!/usr/bin/env bash

echo "========================== Starting Prepare Release Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Identify latest annotated tag (latest version)
export VERSION=$(git describe --abbrev=0 --tags)

mkdir -p deploy_dir

# Create third party license csv file and add it to the deploy directory.
git clone --depth=1 https://github.com/Alfresco/third-party-license-overrides.git
python3 ./third-party-license-overrides/thirdPartyLicenseCSVCreator.py --project "`pwd`" --version "${VERSION}" --combined --output "deploy_dir"

echo "Local deploy directory content:"
ls -lA deploy_dir

popd
set +vex
echo "========================== Finishing Prepare Release Deploy Script =========================="
