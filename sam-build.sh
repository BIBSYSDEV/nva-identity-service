#!/bin/bash
# SAM build wrapper that disables Gradle configuration cache
export GRADLE_OPTS="-Dorg.gradle.configuration-cache=false"
sam build "$@"