#!/usr/bin/env bash
# turn on debug mode
set -x

preconditions() {
  if [[ -f "env.properties.template" ]]; then
    . "env.properties.template"
  else
    echo "Can't find env.properties.template. Maybe forgot to create one in scripts dir?"
    exit 100
  fi

  declare -a requiredVariables=(
    "LOGS_PATH"
    "DATA_PATH"
    "APPS_PATH"
    "SERVICE_NAME"
    "LOG_PATH"
  )

  for requiredVariable in "${requiredVariables[@]}"; do
    if [ -z "${!requiredVariable}" ]; then
      echo "$requiredVariable not set. Please edit env.properties file in deployment directory or provide variable in the environment."
      exit 1
    fi
  done

}

preconditions

# Installs scripts to app working directory on server
# IMPORTANT! Run with: sudo -E ./install.sh
TARGET_DIR="${APPS_PATH}/${SERVICE_NAME}"
mkdir -p ${TARGET_DIR}
mkdir -p ${LOG_PATH}
ln -s ${LOG_PATH} ${TARGET_DIR}/log
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
