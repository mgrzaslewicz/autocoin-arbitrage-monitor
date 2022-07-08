#!/usr/bin/env bash

preconditions() {
    if [[ -z ${LOGS_PATH} ]]; then
        echo "LOGS_PATH not set. Please edit env.properties file in deployment directory.";
        exit 105;
    fi

    if [[ -z ${DATA_PATH} ]]; then
        echo "DATA_PATH not set. Please edit env.properties file in deployment directory.";
        exit 106;
    fi

}

preconditions

# Installs scripts to app working directory on server
# IMPORTANT! Run with: sudo -E ./install.sh
SERVICE_NAME="autocoin-arbitrage-monitor"
TARGET_DIR="${APPS_PATH}/${SERVICE_NAME}"
mkdir -p ${TARGET_DIR}
mkdir -p ${LOGS_PATH}/${SERVICE_NAME}
ln -s ${LOGS_PATH}/${SERVICE_NAME} ${TARGET_DIR}/log
mkdir -p ${DATA_PATH}/${SERVICE_NAME}
ln -s ${DATA_PATH}/${SERVICE_NAME} ${TARGET_DIR}/data
cp start.sh ${TARGET_DIR}
cp stop.sh ${TARGET_DIR}
cp restart.sh ${TARGET_DIR}
cp docker-login.sh ${TARGET_DIR}
cp docker-log.sh ${TARGET_DIR}
cp docker-ps.sh ${TARGET_DIR}
cp env.properties.template ${TARGET_DIR}
chmod +x ${TARGET_DIR}/*.sh
