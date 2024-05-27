#!/bin/bash

# Definir variables
REGISTRY_ORIGEN="registry.gitlab.geocom.com.uy:5005"
REGISTRY_DESTINO="registry-farmacenter.farmacenter.com.py:5005"

USER_ORIGEN=""
TOKEN_ORIGEN=""

USER_DESTINO=""
TOKEN_DESTINO=""

IMAGE_NAME="uy-com-geocom-scm-utils/ansible-ssh"
TAG="ansible-mitogen-2022"

# Origen
docker login $REGISTRY_ORIGEN -u $USER_ORIGEN -p $TOKEN_ORIGEN
echo "Descargando la imagen de $REGISTRY_ORIGEN..."
docker pull $REGISTRY_ORIGEN/$IMAGE_NAME:$TAG

# Destino
echo "Etiquetando la imagen para $REGISTRY_DESTINO..."
docker tag $REGISTRY_ORIGEN/$IMAGE_NAME:$TAG $REGISTRY_DESTINO/$IMAGE_NAME:$TAG

# Subir a destino
echo "Subiendo la imagen a $REGISTRY_DESTINO..."
echo "Docker login"
docker login $REGISTRY_DESTINO -u $USER_DESTINO -p $TOKEN_DESTINO
docker push $REGISTRY_DESTINO/$IMAGE_NAME:$TAG
echo "docker logout"
# Cleaning
#docker rmi $REGISTRY_ORIGEN/$IMAGE_NAME:$TAG
#docker rmi $REGISTRY_DESTINO/$IMAGE_NAME:$TAG
echo "Proceso completado."