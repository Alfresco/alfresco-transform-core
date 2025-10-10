#!/usr/bin/env bash

echo "=========================== Starting Test&Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Always build the image, but only publish from the "master" branch
[ "${PULL_REQUEST}" = "false" ] && [ "${BRANCH_NAME}" = "master" ] && PROFILE="internal" || PROFILE="local"

# If the branch is "master" and the commit is not a Pull Request then deploy the JAR SNAPSHOT artifacts
[ "${PULL_REQUEST}" = "false" ] && [ "${BRANCH_NAME}" = "master" ] && DEPLOY="deploy" || DEPLOY="verify"

# Do not deploy snapshots for alfresco-transform-core, alfresco-transformer-base and alfresco-base-t-engine
mvn -B -U -Dmaven.wagon.http.pool=false \
    clean ${DEPLOY} \
    -DadditionalOption=-Xdoclint:none -Dmaven.javadoc.skip=true \
    -Dparent.core.deploy.skip=true -Dtransformer.base.deploy.skip=true \
    "-P${PROFILE},docker-it-setup,${1}" \
    -DfailIfNoTests=false \
    -Dit.test='**/*IT.java' \
    verify

docker ps -a -q | xargs -r -l docker stop ; docker ps -a -q | xargs -r -l docker rm

popd
set +vex
echo "=========================== Finishing Test&Deploy Script =========================="
