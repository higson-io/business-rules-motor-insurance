#!/usr/bin/env bash

IMAGE_NAME=decerto/business-rules-motor-insurance-hyperon-demo-application
VERSION=4.0.0

USER=$1
PASS=$2

docker login -u ${USER} -p {PASS} 
docker buildx build --platform linux/amd64,linux/arm64  -t ${IMAGE_NAME}:${VERSION} -t ${IMAGE_NAME}:latest --push .
