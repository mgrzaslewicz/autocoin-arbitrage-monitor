#!/bin/bash
# turn on debug mode
set -x

rm pom.xml.releaseBackup
rm release.properties
mvn clean release:prepare git-commit-id:revision
