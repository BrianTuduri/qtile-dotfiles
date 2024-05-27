#!/usr/bin/env sh
#find '/usr/share/nginx/html/' -name '*.js'
#find '/usr/share/nginx/html/' -name '*.js' -exec sed -i -e 's,API_ADDRESS_CONFIGURE,'"$API_ADDRESS"',g' {} \;
nginx -g "daemon off;"