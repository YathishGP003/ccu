#!/bin/bash
set -x #echo on
echo "test bug test https://team75f.atlassian.net/browse/RENATUS-518 test additional forgot dev  in progress"
VERSION_VAR="$(cat VERSION)"
VERSIONID="$(cat VERSIONID)"
echo "$VERSION_VAR"
for FILE1 in "$@"
do
		curl --request PUT \
			--url https://team75f.atlassian.net/rest/api/2/issue/$FILE1 \
			--user ryan2@75f.io:BIpmwOmMfLbTbevVzNqjF5B1 \
			--header 'cache-control: no-cache' \
			--header 'content-type: application/json' \
			--header 'postman-token: 63adb46d-c489-19c0-8709-cc8c1267c6de' \
			--data '{    "update": {        "fixVersions" : [            {"add":                {"name" : "'$VERSION_VAR'"}            }        ]    }}'
		echo "TASKID: " + $FILE1
		curl --request POST --url https://team75f.atlassian.net/rest/api/2/issue/$FILE1/transitions --user ryan2@75f.io:BIpmwOmMfLbTbevVzNqjF5B1 --header 'cache-control: no-cache' --header 'content-type: application/json' --header 'postman-token: f55dcedb-cf92-2592-2a1c-d336e5c0f888' --data '{ "transition": {"id": "71"}}'
done

  curl --request PUT \
	--url https://team75f.atlassian.net/rest/api/2/version/$VERSIONID \
	--user ryan2@75f.io:BIpmwOmMfLbTbevVzNqjF5B1 \
	--header 'cache-control: no-cache' \
	--header 'content-type: application/json' \
	--header 'postman-token: 1ddef207-8481-2573-2873-251846fac0f1' \
	--data '{"released": true}'

rm VERSIONID
