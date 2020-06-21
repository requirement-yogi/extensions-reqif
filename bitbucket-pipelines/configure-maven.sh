#!/bin/bash

echo "Current dir:"
pwd
find .
#echo "Full contents of the Atlassian SDK repo:"
#find /usr/share/atlassian-plugin-sdk-*

echo "Will pick the settings.xml among:"
find /usr/local -name "settings.xml"
find /usr/share/atlassian-plugin-sdk-*/apache-maven-*/conf/ -name "settings.xml"

set -e
set -u
SETTINGS="$(find /usr/share/atlassian-plugin-sdk-*/apache-maven-*/conf/ -name "settings.xml" | head -n 1)"

echo
echo "=== Modifying the file $SETTINGS ==="
echo

cp ./bitbucket-pipelines/custom-settings.xml "$SETTINGS"

# ./bitbucket-pipelines/custom-settings.xml < "$SETTINGS"

sed -i "s/\${MAVEN_USERNAME}/${MAVEN_USERNAME}/g" "$SETTINGS"
sed -i "s/\${MAVEN_PASSWORD}/${MAVEN_PASSWORD}/g" "$SETTINGS"

cat "$SETTINGS"
