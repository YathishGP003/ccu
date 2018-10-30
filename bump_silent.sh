#!/bin/bash
set -x echo on
git reset --hard
git checkout master
git pull
git config --global user.name "usdevrepos"
git config --global user.email "usdevrepos@75f.io"

BASE_STRING=`cat VERSION`
BASE_LIST=(`echo $BASE_STRING | tr '.' ' '`)
V_MAJOR=${BASE_LIST[0]}
V_MINOR=${BASE_LIST[1]}
V_PATCH=${BASE_LIST[2]}
echo "Clear gradle properties"
> gradle.properties

echo "Current version : $BASE_STRING"
V_MINOR=$((V_MINOR + 1))
V_PATCH=0
SUGGESTED_VERSION="$V_MAJOR.$V_MINOR.$V_PATCH"
INPUT_STRING=$SUGGESTED_VERSION
echo "Will set new version to be $INPUT_STRING"
echo $INPUT_STRING > VERSION
echo "Version $INPUT_STRING:" > tmpfile
echo "http://updates.75fahrenheit.com/CCUV2_SN_dev_$V_MAJOR.$V_MINOR.apk" > tmpfile
git log --pretty=format:" - %s" "v$BASE_STRING"...HEAD >> tmpfile
echo "" >> tmpfile
echo "" >> tmpfile
#No longer keeping permanant log because we are using build release process.
#cat CHANGES >> tmpfile
mv tmpfile CHANGES
echo "Write gradle properties"
echo "VERSION_CODE_MINOR=$V_MINOR" >> gradle.properties
echo "VERSION_CODE_MAJOR=$V_MAJOR" >> gradle.properties
echo "VERSION_CODE_PATCH=$V_PATCH" >> gradle.properties
echo "org.gradle.jvmargs=-Xmx2560M" >> gradle.properties
echo "applicationName=CCUV2_SN" >> gradle.properties
git add gradle.properties
git add VERSION
git add CHANGES
git commit -m "Version bump to $INPUT_STRING"
git push http://usdevrepos:mDRg-9kqCpmuypoxR1GZ@gitlab.com/75f/android/ccu-v3.git master

git tag -a -m "Tagging version $INPUT_STRING" "v$INPUT_STRING"
git push http://usdevrepos:mDRg-9kqCpmuypoxR1GZ@gitlab.com/75f/android/ccu-v3.git --tags
