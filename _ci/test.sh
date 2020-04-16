#!/usr/bin/env bash

echo "=========================== Starting Test&Deploy Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../"

# Always build the image, but only publish from the "master" branch
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ] && PROFILE="internal" || PROFILE="local"

# If the branch is "master" and the commit is not a Pull Request then deploy the JAR SNAPSHOT artifacts
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ] && DEPLOY="deploy" || DEPLOY="test"

# If the branch is "master" and the profile provided is not "aio-test" do not deploy alfresco-transformer-base
[ "${1}" != "aio-test" ] && [ "${TRAVIS_BRANCH}" = "master" ] && SKIP_DUP_DEPLOY="-Dparent.deploy.skip=true -Dtransformer.base.deploy.skip=true" || SKIP_DUP_DEPLOY=""

mvn -B -U \
    clean ${DEPLOY} \
    -DadditionalOption=-Xdoclint:none -Dmaven.javadoc.skip=true \
    "-P${PROFILE},docker-it-setup,${1}" ${SKIP_DUP_DEPLOY}

docker ps -a -q | xargs -r -l docker stop ; docker ps -a -q | xargs -r -l docker rm

popd
set +vex
echo "=========================== Finishing Test&Deploy Script =========================="

