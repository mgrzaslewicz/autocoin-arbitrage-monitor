#!/usr/bin/env bash

preconditions() {
  if [[ -f "env.properties" ]]; then
    . "env.properties"
  else
    echo "Can't find env.properties. Maybe forgot to create one in scripts dir?"
    exit 100
  fi

  declare -a requiredVariables=(
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

# Kill container if it is already running
running_image_id="$(docker ps -a --filter "name=${SERVICE_NAME}" -q)"
if [ ! -z "$running_image_id" ]; then
  echo 'Killing previous version of container'
  docker rm -f ${running_image_id}
else
  echo 'No container running. Skipping...'
fi
