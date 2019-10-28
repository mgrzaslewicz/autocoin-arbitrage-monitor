#!/usr/bin/env bash
# Kill container if it is already running
running_image_id="$(docker ps -a --filter "name=autocoin-arbitrage-monitor" -q)";
if [ ! -z "$running_image_id" ]; then
    echo 'Killing previous version of container'
    docker rm -f ${running_image_id}
else
    echo 'No container running. Skipping...'
fi