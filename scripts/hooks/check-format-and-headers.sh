#!/usr/bin/env bash

set +x

mvn spotless:apply validate -DlicenseUpdateHeaders=true > /dev/null || true

set -x
