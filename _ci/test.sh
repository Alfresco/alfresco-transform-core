#!/usr/bin/env bash

echo "=========================== Starting Test&Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Always build the image, but only publish from the "master" branch
[ "${PULL_REQUEST}" = "false" ] && [ "${BRANCH_NAME}" = "master" ] && PROFILE="internal" || PROFILE="local"

# If the branch is "master" and the commit is not a Pull Request then run the deploy phase to push the images
[ "${PULL_REQUEST}" = "false" ] && [ "${BRANCH_NAME}" = "master" ] && MAVEN_PHASE="deploy" || MAVEN_PHASE="verify"

if [ "${SMOKE_TESTS}" = "true" ]; then
  ADDITIONAL_MAVEN_OPTS="-Dit.test=**/*IT.java"
else
  ADDITIONAL_MAVEN_OPTS=""
fi

if [ "${SKIP_DEPLOY}" = "true" ]; then
  MAVEN_PHASE="verify"
fi

source "$(dirname "${BASH_SOURCE[0]}")/java_base_image_map.sh"

# Never deploy the SNAPSHOT jars: there is no snapshot repository to deploy them to
mvn -B -U \
    clean ${MAVEN_PHASE} \
    -DadditionalOption=-Xdoclint:none -Dmaven.javadoc.skip=true \
    -Dmaven.deploy.skip=true -Dparent.core.deploy.skip=true -Dtransformer.base.deploy.skip=true \
    "-P${PROFILE},docker-it-setup,${1}" \
    ${ADDITIONAL_MAVEN_OPTS}

docker ps -a -q | xargs -r -l docker stop ; docker ps -a -q | xargs -r -l docker rm || true

popd
set +vex
echo "=========================== Finishing Test&Deploy Script =========================="
