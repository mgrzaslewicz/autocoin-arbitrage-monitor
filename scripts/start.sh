#!/usr/bin/env bash

preconditions() {
  if [[ -f "env.properties" ]]; then
    . "env.properties"
  else
    echo "Can't find env.properties. Maybe forgot to create one in scripts dir?"
    exit 100
  fi

  declare -a requiredVariables=(
    "APP_DATA_PATH"
    "APP_OAUTH_CLIENT_ID"
    "APP_OAUTH_CLIENT_SECRET"
    "DOCKER_PORT"
    "HOST_PORT"
    "LOG_PATH"
    "SERVICE_NAME"
  )

  for requiredVariable in "${requiredVariables[@]}"; do
    if [ -z "${!requiredVariable}" ]; then
      echo "$requiredVariable not set. Please edit env.properties file in deployment directory."
      exit 1
    fi
  done

}

preconditions

VERSION_TAG="${SERVICE_NAME}-${VERSION}"

# Run new container
echo "Starting new version of container. Using version: ${VERSION}"
echo "Exposing docker port ${DOCKER_PORT} to host port ${HOST_PORT}"

# Use JAVA_OPTS="-XX:+ExitOnOutOfMemoryError" to prevent from running when any of threads runs of out memory and dies

docker run --name ${SERVICE_NAME} -d \
-p ${HOST_PORT}:${DOCKER_PORT} \
-e BASIC_PASS=${BASIC_PASS} \
-e DOCKER_TAG=${VERSION_TAG} \
-e APP_OAUTH_CLIENT_ID=${APP_OAUTH_CLIENT_ID} \
-e APP_OAUTH_CLIENT_SECRET=${APP_OAUTH_CLIENT_SECRET} \
-v ${LOG_PATH}:/app/log \
-v ${APP_DATA_PATH}:/app/data \
--memory=1400m \
--restart=no \
--network autocoin-services-admin \
localhost:5000/${SERVICE_NAME}:${VERSION_TAG}
