#!/bin/bash

## File required by bitbucket

sed -i~ "/<servers>/ a\
<server>\
  <id>private-repo</id>\
  <username>${MAVEN_USERNAME}</username>\
  <password>${MAVEN_PASSWORD}</password>\
</server>" /usr/share/maven/conf/settings.xml

sed -i "/<profiles>/ a\
<profile>\
  <id>private-repo</id>\
  <activation>\
    <activeByDefault>true</activeByDefault>\
  </activation>\
  <repositories>\
    <repository>\
      <id>atlassian-public</id>\
      <url>https://maven.atlassian.com/repository/public</url>\
    </repository>\
  </repositories>\
  <pluginRepositories>\
    <pluginRepository>\
      <id>atlassian-public-2</id>\
      <url>https://maven.atlassian.com/repository/public</url>\
    </pluginRepository>\
  </pluginRepositories>\
</profile>" /usr/share/maven/conf/settings.xml