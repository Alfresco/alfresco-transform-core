#!/usr/bin/env bash

if [ -n "$JAVA_RUNTIME_VERSION" ]; then
  declare -A JAVA_IMAGE_MAP=(
    [21]="alfresco/alfresco-base-java:jre21-rockylinux9@sha256:27297bffe7d45152194f08a8f38917a7e3d43b9a34dcfcdc29b83a80c4c33a10"
    [25]="alfresco/alfresco-base-java:jre25-rockylinux9@sha256:7cc61edc5444e0eb69cfbf5d51716666ab8605fe9c8fdba75407e9484185fcd9"
  )
  JAVA_BASE_IMAGE="${JAVA_IMAGE_MAP[$JAVA_RUNTIME_VERSION]}"
  export JAVA_BASE_IMAGE
fi
