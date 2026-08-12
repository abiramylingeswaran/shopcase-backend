#!/bin/sh
set -e
MAVEN_VERSION=3.9.9
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION"
MVN_CMD="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MVN_CMD" ]; then
  echo "Downloading Apache Maven $MAVEN_VERSION..."
  ZIP="/tmp/apache-maven-$MAVEN_VERSION-bin.zip"
  mkdir -p "$HOME/.m2/wrapper/dists"
  curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip" -o "$ZIP"
  unzip -q -o "$ZIP" -d "$HOME/.m2/wrapper/dists"
  rm -f "$ZIP"
fi

exec "$MVN_CMD" "$@"