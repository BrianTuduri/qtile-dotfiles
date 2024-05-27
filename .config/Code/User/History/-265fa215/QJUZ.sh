#!/bin/bash

BASE_IMAGE=${1:-"node:10.15.0-stretch"}

cat > Dockerfile <<EOF
FROM nexus.geocom.com.uy/$BASE_IMAGE


#UNZIPPER ############################
FROM busybox:1.31.1 AS unzipper
ENV APP_NAME="order-tracker-frontend"
COPY $APP_NAME.zip .
RUN unzip $APP_NAME.zip -d /root

#RUNNER ############################

FROM nginx:alpine

ENV APP_NAME="order-tracker-frontend"
ENV API_ADDRESS="localhost:8080"

#Copy $APP_NAME files
COPY --from=unzipper /root/dist/$APP_NAME/ /usr/share/nginx/html
COPY CI/nginx.conf /etc/nginx/conf.d/default.conf
COPY CI/replaceAndRun.sh .
RUN chmod +x replaceAndRun.sh

# app port 80
EXPOSE 80

# Run Nginx
CMD ["sh", "replaceAndRun.sh"]
EOF