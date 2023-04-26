#!/usr/bin/env bash

PROPERTY_FILE="${PROPERTY_FILE:=env.properties}"

preconditions() {
  declare -a requiredVariables=(
    "SERVICE_NAME"
  )

  echo "Expecting variables ${requiredVariables[*]} to be provided by the environment or $PROPERTY_FILE file"

  if [[ -f "$PROPERTY_FILE" ]]; then
    echo "Loading variables from $PROPERTY_FILE"
    . "$PROPERTY_FILE"
  else
    echo "Can't find $PROPERTY_FILE. Expecting variables to be provided by the environment"
  fi

  for requiredVariable in "${requiredVariables[@]}"; do
    if [ -z "${!requiredVariable}" ]; then
      echo "$requiredVariable not set. Provide it via $PROPERTY_FILE file in deployment directory or env variable."
      exit 1
    fi
  done
}

preconditions

# Kill container if it is already running
running_image_id="$(docker ps -a --filter "name=${SERVICE_NAME}" -q)"
if [ -n "$running_image_id" ]; then
  echo 'Killing previous version of container'
  docker rm -f "${running_image_id}"
else
  echo 'No container running. Skipping...'
fi
