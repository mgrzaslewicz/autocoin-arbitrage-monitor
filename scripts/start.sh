#!/usr/bin/env bash

preconditions() {
    if [[ -f "env.properties" ]]
    then
        . "env.properties"
    else
        echo "Can't find env.properties. Maybe forgot to create one in scripts dir?"
        exit 100
    fi

    if [[ -z ${VERSION} ]]; then
        echo "VERSION not set. Please run the script with VERSION variable set: VERSION=x.x.x sudo -E ./start.sh";
        exit 101;
    else
        VERSION="autocoin-arbitrage-monitor-${VERSION}";
    fi

    if [[ -z ${APP_PORT_ON_HOST} ]]; then
        echo "APP_PORT_ON_HOST not set. Please edit env.properties file in deployment directory.";
        exit 104;
    fi

    if [[ -z ${LOG_PATH} ]]; then
        echo "LOG_PATH not set. Please edit env.properties file in deployment directory.";
        exit 105;
    fi

    if [[ -z ${APP_DATA_PATH} ]]; then
        echo "APP_DATA_PATH not set. Please edit env.properties file in deployment directory.";
        exit 106;
    fi

    if [[ -z ${SERVICE_NAME} ]]; then
        echo "SERVICE_NAME not set. Please edit env.properties file in deployment directory.";
        exit 107;
    fi

    if [[ -z ${APP_OAUTH_CLIENT_ID} ]]; then
        echo "APP_OAUTH_CLIENT_ID not set. Please edit env.properties file in deployment directory.";
        exit 107;
    fi

    if [[ -z ${APP_OAUTH_CLIENT_SECRET} ]]; then
        echo "APP_OAUTH_CLIENT_SECRET not set. Please edit env.properties file in deployment directory.";
        exit 107;
    fi
}

preconditions

# Run new container
echo "Starting new version of container. Using version: ${VERSION}";
echo "Using port: ${APP_PORT_ON_HOST}";

# Use JAVA_OPTS="-XX:+ExitOnOutOfMemoryError" to prevent from running when any of threads runs of out memory and dies

docker run --name ${SERVICE_NAME} -d \
    -p ${APP_PORT_ON_HOST}:10021 \
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
