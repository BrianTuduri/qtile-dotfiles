#!/bin/bash

BASE_IMAGE=${1:-"node:10.15.0-stretch"}

cat > Dockerfile <<EOF
FROM nexus.geocom.com.uy/$BASE_IMAGE
WORKDIR /usr/src/app

COPY . .

RUN npm install

EXPOSE 3001
CMD [ "node", "app.js" ]
EOF