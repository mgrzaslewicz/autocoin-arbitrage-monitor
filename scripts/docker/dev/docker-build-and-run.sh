#!/bin/bash
set -x # turn on debug mode
set -e # exit on any error


function startDocker() {
  LOG_PATH="$(realpath log)" \
  APP_DATA_PATH="$(realpath data)" \
  VERSION="latest" \
  SERVICE_NAME="autocoin-arbitrage-monitor" \
  DOCKER_REGISTRY="" \
  HOST_PORT="9001" \
  DOCKER_PORT="9001" \
  OAUTH_CLIENT_SECRET="autocoin-arbitrage-monitor-secret-for-local-development" \
  METRICS_DESTINATION="FILE" \
    ./scripts/docker/run/docker-start.sh
}

./scripts/docker/build/docker-build.sh
stopAndRemoveDockerContainer
startDocker
