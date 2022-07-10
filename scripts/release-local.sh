#!/bin/bash
# turn on debug mode
set -x

cd ..
# git.commit.id.describe set to 'local' to have consistent version of docker image in local builds
mvn clean package git-commit-id:revision dockerfile:build dockerfile:push -P local-release -DskipTests
