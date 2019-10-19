#!/bin/bash

## File required by bitbucket

sed -i~ "/<servers>/ a\
<server>\
  <id>ry-releases</id>\
  <username>${MAVEN_USERNAME}</username>\
  <password>${MAVEN_PASSWORD}</password>\
</server>" /usr/share/maven/conf/settings.xml

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
</profile>" /usr/share/maven/conf/settings.xml