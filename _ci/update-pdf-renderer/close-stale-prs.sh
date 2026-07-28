#!/usr/bin/env bash
# Closes every other open PR previously opened by this workflow, leaving only
# the PR just created/updated in this run open.
set -euo pipefail

CURRENT_BRANCH="${NEW_PR_BRANCH}"
gh pr list --state open --limit 100 \
  --json number,headRefName \
  --jq '.[] | select(.headRefName | startswith("auto/update-pdf-renderer-")) | [.number, .headRefName] | @tsv' \
| while IFS=$'\t' read -r NUMBER BRANCH; do
    if [ "${BRANCH}" != "${CURRENT_BRANCH}" ]; then
      echo "Closing stale PR #${NUMBER} (${BRANCH})"
      gh pr close "${NUMBER}" \
        --comment "Superseded by #${NEW_PR} — update to PDF Renderer ${LATEST}." \
        --delete-branch
    fi
  done
