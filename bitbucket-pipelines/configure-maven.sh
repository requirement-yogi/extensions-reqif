#!/bin/bash

## File required by bitbucket

find /usr/local -name "settings.xml"

echo "Will pick the settings.xml among:"
find /usr/share/atlassian-plugin-sdk-*/apache-maven-*/conf/ -name "settings.xml"

set -e
set -u
SETTINGS="$(find /usr/share/atlassian-plugin-sdk-*/apache-maven-*/conf/ -name "settings.xml" | head -n 1)"

echo "Modifying the file $SETTINGS"

sed -i~ "/<servers>/ a\
<server>\
  <id>ry-releases</id>\
  <username>${MAVEN_USERNAME}</username>\
  <password>${MAVEN_PASSWORD}</password>\
</server>" "$SETTINGS"

sed -i "/<profiles>/ a\
<profile>\
  <id>ry-releases</id>\
  <activation>\
    <activeByDefault>true</activeByDefault>\
  </activation>\
  <repositories>\
    <repository>\
      <id>ry-releases</id>\
      <url>https://maven.play-sql.com/repository/ry-releases/</url>\
    </repository>\
  </repositories>\
</profile>" "$SETTINGS"

cat "$SETTINGS"