#!/usr/bin/env bash

IMAGE_NAME="node-nvm"
IMAGE_TAG=$1
DOCKER_REGISTRY="registry.gitlab.geocom.com.uy:5005/scm-docker-images"

if [ -z "$DOCKER_REGISTRY" ]; then
    FULL_IMAGE_NAME="$IMAGE_NAME:$IMAGE_TAG"
else
    FULL_IMAGE_NAME="$DOCKER_REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
fi

docker build -t $FULL_IMAGE_NAME .

if [ $? -eq 0 ]; then
    echo "Docker Image Built Successfully."
else
    echo "Error Building Docker Image."
    exit 1
fi

docker push $FULL_IMAGE_NAME

if [ $? -eq 0 ]; then
    echo "Docker Image Pushed Successfully."
else
    echo "Error Pushing Docker Image."
    exit 1
fi
