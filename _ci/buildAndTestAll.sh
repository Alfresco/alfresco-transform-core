#!/usr/bin/env bash

set -e

mvn -U -Dmaven.wagon.http.pool=false clean install -DadditionalOption=-Xdoclint:none -Dmaven.javadoc.skip=true -Dparent.core.deploy.skip=true -Dtransformer.base.deploy.skip=true -Plocal,docker-it-setup