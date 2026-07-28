#!/usr/bin/env bash

set -euo pipefail

TAGS=$(gh api --paginate repos/Alfresco/alfresco-pdf-renderer/tags --jq '.[].name')
LATEST=$(printf '%s\n' "${TAGS}" \
  | grep -E '^[0-9]+\.[0-9]+\.[0-9]+-[0-9]+$' \
  | sort -V \
  | tail -n1 || true)

if [ -z "${LATEST}" ]; then
  echo "::error::Could not determine the latest alfresco-pdf-renderer release tag"
  exit 1
fi

CURRENT_PDF=$(grep -oP '^ARG PDF_RENDERER_VERSION=\K.*' engines/pdfrenderer/Dockerfile)
CURRENT_AIO=$(grep -oP '^ARG PDF_RENDERER_VERSION=\K.*' engines/aio/Dockerfile)
if [ "${CURRENT_PDF}" != "${CURRENT_AIO}" ]; then
  echo "::error::PDF_RENDERER_VERSION differs between Dockerfiles (pdfrenderer=${CURRENT_PDF}, aio=${CURRENT_AIO}); please reconcile them first"
  exit 1
fi
CURRENT="${CURRENT_PDF}"

echo "Latest alfresco-pdf-renderer release: ${LATEST}"
echo "Current version in Dockerfiles:       ${CURRENT}"

echo "latest=${LATEST}" >> "$GITHUB_OUTPUT"
if [ "${LATEST}" = "${CURRENT}" ]; then
  echo "Already up to date; nothing to do."
  echo "changed=false" >> "$GITHUB_OUTPUT"
else
  echo "changed=true" >> "$GITHUB_OUTPUT"
fi
