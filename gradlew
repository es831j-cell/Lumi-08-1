#!/bin/sh
set -eu
GRADLE_VERSION="8.9"
CACHE_ROOT="${HOME:-/tmp}/.gradle/lumi-wrapper"
GRADLE_HOME="$CACHE_ROOT/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$CACHE_ROOT"
  ZIP="$CACHE_ROOT/gradle-$GRADLE_VERSION-bin.zip"
  echo "Lumi bootstrap: downloading Gradle $GRADLE_VERSION..."
  curl -fL --retry 3 --retry-delay 2 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  rm -rf "$GRADLE_HOME"
  unzip -q "$ZIP" -d "$CACHE_ROOT"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
