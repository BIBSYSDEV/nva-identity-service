#!/bin/bash
# Wrapper script for running `sam build` locally
export GRADLE_OPTS="-Dorg.gradle.configuration-cache=false"
sam build "$@"
