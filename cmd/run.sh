#!/usr/bin/env sh

set -e

exec java -Djava.security.egd=file:/dev/./urandom ${JAVA_OPTS} -cp . org.springframework.boot.loader.launch.JarLauncher