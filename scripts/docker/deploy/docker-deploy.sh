#!/bin/bash
set -e
set -x

SERVICE_NAME="autocoin-arbitrage-monitor"
TMP="/tmp"
TAR_NAME="${SERVICE_NAME}-${VERSION}.tar"
TAR_GZ_NAME="${SERVICE_NAME}-${VERSION}.tar.gz"
TAR_PATH="${TMP}/$TAR_NAME"
TAR_GZ_PATH="${TAR_PATH}.gz"
SSH_USER_HOST="${SSH_USER}@${DESTINATION_HOST}"
SSH_PORT="${SSH_PORT:=22}"
APP_PATH="${APP_PATH:=/opt/autocoin/apps/$SERVICE_NAME}"
APP_RUN_SCRIPTS_PATH="${APP_RUN_SCRIPTS_PATH:=$APP_PATH/run}"

SSH_CONNECTION_TIMEOUT_SECONDS=5

preconditions() {
  declare -a requiredVariables=(
    "DESTINATION_HOST"
    "SSH_USER"
    "SSH_KEY"
    "VERSION"
  )

  echo "Expecting variables ${requiredVariables[*]} to be provided by the environment"

  for requiredVariable in "${requiredVariables[@]}"; do
    if [ -z "${!requiredVariable}" ]; then
      echo "$requiredVariable not set. Provide it via env variable."
      exit 1
    fi
  done
}


checkIfVersionExists() {
  if [[ $(docker images -q "${SERVICE_NAME}:${VERSION}" 2>/dev/null) == "" ]]; then
    echo "Image ${SERVICE_NAME}:${VERSION} does not exist. Please build it first."
    exit 1
  fi
}

prepareImageArchive() {
  echo "Preparing image archive"
  rm -f "${TAR_PATH}"
  rm -f "${TAR_GZ_PATH}"
  docker save "${SERVICE_NAME}:${VERSION}" > "${TAR_PATH}"
  gzip "${TAR_PATH}"
}

uploadImageArchive() {
  echo "Uploading image archive"
  scp -o ConnectTimeout="${SSH_CONNECTION_TIMEOUT_SECONDS}" -i "${SSH_KEY}" -P "${SSH_PORT}" "${TAR_GZ_PATH}" "${SSH_USER_HOST}:${TAR_GZ_PATH}"
}

unpackImageArchiveOnDestinationHost() {
  echo "Unpacking image archive on destination host"
  ssh -o ConnectTimeout="${SSH_CONNECTION_TIMEOUT_SECONDS}" -t -p "${SSH_PORT}" -i "${SSH_KEY}" "${SSH_USER_HOST}" "gunzip -f ${TAR_GZ_PATH}"
}

createImageOnDestinationHost() {
  echo "Creating image on destination host"
  ssh -o ConnectTimeout="${SSH_CONNECTION_TIMEOUT_SECONDS}" -t -p "${SSH_PORT}" -i "${SSH_KEY}" "${SSH_USER_HOST}" "docker load -i ${TAR_PATH}"
}

copyRunScripts() {
  echo "Copying run scripts"
  ssh -o ConnectTimeout="${SSH_CONNECTION_TIMEOUT_SECONDS}" -t -p "${SSH_PORT}" -i "${SSH_KEY}" "${SSH_USER_HOST}" "rm -f ${APP_RUN_SCRIPTS_PATH}/*.sh ; sudo mkdir -p ${APP_RUN_SCRIPTS_PATH} && sudo chown -R $SSH_USER ${APP_RUN_SCRIPTS_PATH}"
  ssh -o ConnectTimeout="${SSH_CONNECTION_TIMEOUT_SECONDS}" -t -p "${SSH_PORT}" -i "${SSH_KEY}" "${SSH_USER_HOST}" "docker run -v ${APP_RUN_SCRIPTS_PATH}:/app/run -it --entrypoint /scripts/copy-run-scripts.sh ${SERVICE_NAME}:${VERSION}"
}

updateVersionOnDestinationHost() {
  echo "Updating version on destination host"
  ssh -o ConnectTimeout="${SSH_CONNECTION_TIMEOUT_SECONDS}" -t -p "${SSH_PORT}" -i "${SSH_KEY}" "${SSH_USER_HOST}" "sed -i 's/^VERSION=\".*\"$/VERSION=\"$VERSION\"/g' $APP_PATH/env.properties"
}

preconditions
checkIfVersionExists
prepareImageArchive
uploadImageArchive
unpackImageArchiveOnDestinationHost
createImageOnDestinationHost
copyRunScripts
updateVersionOnDestinationHost
