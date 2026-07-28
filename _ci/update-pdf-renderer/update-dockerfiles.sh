#!/usr/bin/env bash
# Rewrites the pinned ARG PDF_RENDERER_VERSION in both Dockerfiles.
# Requires: LATEST.
set -euo pipefail

sed -i "s/^ARG PDF_RENDERER_VERSION=.*/ARG PDF_RENDERER_VERSION=${LATEST}/" \
  engines/pdfrenderer/Dockerfile \
  engines/aio/Dockerfile
git --no-pager diff engines/pdfrenderer/Dockerfile engines/aio/Dockerfile
