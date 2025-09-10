# syntax=docker/dockerfile:1

FROM        252117944295.dkr.ecr.ap-southeast-1.amazonaws.com/platform-images/maven-jdk-21:latest-20240904-wolfi@sha256:807df79812a07f17acb331aa0c20d4d13638ca31a875693363d228124c5add86 as source
WORKDIR     /home/nonroot
ARG         rootProjectName=chatbot-services
ARG         packageExtension=jar
COPY        --chown=65532:65532 . .
RUN         --mount=type=cache,target=/home/nonroot/.gradle,uid=65532,gid=65532,id=gradle_dep ./gradlew clean build -x test --info
RUN         cmd/unzip.sh ${rootProjectName} ${packageExtension}

FROM        252117944295.dkr.ecr.ap-southeast-1.amazonaws.com/platform-images/jdk21-jre:latest-20240904-wolfi@sha256:91b14524772df96d3071bcc7f3a1f912919489120b62b43758118146d9a1a55c
WORKDIR     /app
COPY        --from=source  --chown=65532:65532 /home/nonroot/cmd/run.sh cmd/run.sh
COPY        --from=source  --chown=65532:65532 /home/nonroot/unzip ./

CMD         ["cmd/run.sh"]