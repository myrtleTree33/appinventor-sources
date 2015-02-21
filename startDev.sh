#!/bin/bash 

APP_ENGINE_PATH=/home/joel/repos/appengine-java-sdk-1.9.17

## Run the server
cd appinventor/
${APP_ENGINE_PATH}/bin/dev_appserver.sh --port=8888 --address=0.0.0.0 appengine/build/war/ 
