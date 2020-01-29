#!/usr/bin/env bash

preconditions() {
  if [[ -f "env.properties" ]]; then
    . "./env.properties"
  else
    echo "env.properties not found"
    exit 9
  fi

  declare -a requiredVariables=(
    "APP_PORT_ON_HOST"
    "LOG_PATH"
    "APP_DATA_PATH"
    "SERVICE_NAME"
    "APP_OAUTH_CLIENT_ID"
    "APP_OAUTH_CLIENT_SECRET"
  )

  for requiredVariable in "${requiredVariables[@]}"; do
    if [ -z "$requiredVariable" ]; then
      echo "$requiredVariable not set. Please edit env.properties file in deployment directory."
      exit 99
    fi
  done

  if [[ -z "$VERSION" ]]; then
    VERSION="latest"
  else
    VERSION="${SERVICE_NAME}-${VERSION}"
  fi
}

preconditions

# Run new container
echo "Starting new version of container. Using version: ${VERSION}"
echo "Using port: ${APP_PORT_ON_HOST}"

# Use JAVA_OPTS="-XX:+ExitOnOutOfMemoryError" to prevent from running when any of threads runs of out memory and dies

docker run --name ${SERVICE_NAME} -d \
  -p 127.0.0.1:${APP_PORT_ON_HOST}:10021 \
  -e BASIC_PASS=${BASIC_PASS} \
  -e DOCKER_TAG=${VERSION} \
  -e JAVA_OPTS="-XX:+ExitOnOutOfMemoryError" \
  -e APP_OAUTH_CLIENT_ID=${APP_OAUTH_CLIENT_ID} \
  -e APP_OAUTH_CLIENT_SECRET=${APP_OAUTH_CLIENT_SECRET} \
  -v ${LOG_PATH}:/app/log \
  -v ${APP_DATA_PATH}:/app/data \
  --memory=1200m \
  --restart=no \
  --network autocoin-services-admin \
  localhost:5000/autocoin-arbitrage-monitor:${VERSION}
