#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

KEY=~/.ssh/id_rsa

HOST="finanzio.gabro.me"

echo
echo "Deploying"
echo "- using host '$HOST'"
echo "- using SSH key '$KEY'"

# echo
# echo Copying config files...
# scp -ri $KEY $DIR/config ubuntu@$HOST:

echo
echo Downloading fresh copy of nginx.tmpl...
# see https://github.com/JrCs/docker-letsencrypt-nginx-proxy-companion/blob/master/README.md)
ssh -i $KEY ubuntu@$HOST 'curl https://raw.githubusercontent.com/jwilder/nginx-proxy/master/nginx.tmpl > config/nginx.tmpl'

echo
echo Copying docker-compose file...
scp -ri $KEY $DIR/../terraform/docker-compose.yml ubuntu@$HOST:docker-compose.yml

# echo
# echo Writing variables to .env...
# ssh -i $KEY ubuntu@$HOST "echo -e \"$DOTENV\" >.env && cat .env"

echo
echo Updating docker images...
ssh -i $KEY ubuntu@$HOST "docker-compose pull"

echo
echo Removing old containers...
ssh -i $KEY ubuntu@$HOST "docker-compose down"

echo
echo Restarting all the containers...
ssh -i $KEY ubuntu@$HOST "docker-compose up -d"

echo
echo Cleaning up unused resources...
ssh -i $KEY ubuntu@$HOST "docker system prune --all -f"

echo
echo "All done :)"
echo "Please allow about 1 minute for the app to restart"
echo "https://$HOST"
echo
