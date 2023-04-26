#!/bin/bash
set -x
SERVICE_NAME="autocoin-arbitrage-monitor"

function mavenPackageIfNeeded() {
  if compgen -G "target/*.jar" >/dev/null; then
    JAR_FILE_EXISTS="true"
  fi

  if [[ "$SKIP_MAVEN" == "true" && "$JAR_FILE_EXISTS" != "true" ]]; then
    echo "SKIP_MAVEN is set to true, but no jar file found in target directory."
    SKIP_MAVEN="false"
  fi

  if [[ "$SKIP_MAVEN" == "true" ]]; then
    echo "Skipping maven"
  else
    mvn package
  fi
}

function getGitVersionFromJar() {
  tempDir=$(mktemp -d)
  version=$(unzip -p target/*.jar -d "${tempDir}" git.properties | grep "git.commit.id.describe=" | sed "s/git.commit.id.describe=//g" | sed "s/${SERVICE_NAME}-//g")
#  rm -rf "${tempDir}"
  echo "$version"
}

function buildDocker() {
  docker build -t $SERVICE_NAME -t "${SERVICE_NAME}:$1" .
}

function stopAndRemoveDockerContainer() {
  SERVICE_NAME="autocoin-arbitrage-monitor" ./scripts/docker/run/docker-stop-and-remove.sh
}

mavenPackageIfNeeded
buildDocker $(getGitVersionFromJar)
