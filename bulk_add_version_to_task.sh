#!/bin/bash
set -x #echo on
test_data="$(egrep -o "['RENATUS']{3}[-+][1-9][0-9]{0,4}" CHANGES | awk 1 ORS=" ")"
echo "${test_data}"
./add_version_to_task.sh ${test_data[@]}
