#!/bin/bash

BASE_IMAGE=${1:-"nexus.geocom.com.uy/node:10.15.0-stretch"}

cat > Dockerfile <<EOF
FROM $BASE_IMAGE
WORKDIR /usr/src/app

COPY . .

RUN npm install

EXPOSE 3001
CMD [ "node", "app.js" ]
EOF
