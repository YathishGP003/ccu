#!/bin/bash
set -x #echo on
VERSION="$(cat VERSION)"
echo "VERSION: $VERSION"
DATE_STRING=$(date +%Y-%m-%d)
echo $DATE_STRING
VERSION_ID=$(curl --request POST \
  --url https://team75f.atlassian.net/rest/api/2/version \
  --user ryan2@75f.io:BIpmwOmMfLbTbevVzNqjF5B1 \
  --header 'cache-control: no-cache' \
  --header 'content-type: application/json' \
  --header 'postman-token: 1ddef207-8481-2573-2873-251846fac0f1' \
  --data '{    "description": "An excellent version",    "name": "'$VERSION'",    "archived": false,    "released": false,    "releaseDate":"'$DATE_STRING'",    "project": "CCU",    "projectId": 11100}' |  jq -r '.id')

  echo $VERSION_ID > VERSIONID

