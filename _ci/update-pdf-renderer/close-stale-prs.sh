#!/usr/bin/env bash
# Closes any open PRs previously opened by this workflow for older versions,
# leaving only the current version's PR open.
# Requires: GH_TOKEN, GH_REPO, LATEST, NEW_PR.
set -euo pipefail

CURRENT_BRANCH="auto/update-pdf-renderer-${LATEST}"
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
