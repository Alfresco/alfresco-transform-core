#!/usr/bin/env bash

if [[ -z ${GITHUB_MODIFIED_FILES} ]]
then
  modified_files=$(git diff --cached --name-only --diff-filter=ACMR)
else
  modified_files=${GITHUB_MODIFIED_FILES}
fi

if [[ -z ${modified_files} ]]
then
  echo "No modified files, exiting."
  exit 0
fi

include_list=""
for file in ${modified_files}
do
  include_list="${include_list},${file}"
done
include_list=${include_list:1}

mvn spotless:apply validate -DlicenseUpdateHeaders=true -Dspotless-include-list="${include_list}" > /dev/null || true
