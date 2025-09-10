#!/usr/bin/env sh
set -e
if [ -z "$1" ]; then
  echo "Check Dockerfile, must enter project name to unzip"
  exit 1
fi
projectName=$1
version=$(grep "^version=" gradle.properties | cut -d'=' -f2)
unzip -q build/libs/${projectName}-${version}.jar -d unzip