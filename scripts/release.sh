#!/bin/bash

cd ..

mvn clean releaser:release git-commit-id:revision dockerfile:build dockerfile:push -P release -DskipTests