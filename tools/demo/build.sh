#!/usr/bin/env bash
# Build oak-blob-cloud-azure (and the Oak modules it needs) into ~/.m2, then the demo client.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../.." && pwd)
MIRROR=https://maven-central.storage-download.googleapis.com/maven2
if ! command -v mvn >/dev/null 2>&1; then
  export PATH="$HOME/tools/apache-maven-3.9.9/bin:$PATH"
  if ! command -v mvn >/dev/null 2>&1; then
    echo "installing Maven 3.9.9 into ~/tools" >&2
    mkdir -p "$HOME/tools"
    curl -fsSL "$MIRROR/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz" | tar -xz -C "$HOME/tools"
  fi
fi
if [ ! -f "$HOME/.m2/settings.xml" ]; then
  # repo1.maven.org rate-limits cloud VMs; use the Google-hosted Central mirror
  mkdir -p "$HOME/.m2"
  cat > "$HOME/.m2/settings.xml" <<XML
<settings>
  <mirrors>
    <mirror><id>gcs-central</id><mirrorOf>central</mirrorOf><url>$MIRROR</url></mirror>
  </mirrors>
</settings>
XML
fi
MVN_FLAGS=(-q -DskipTests -Dskip.coverage=true -Drat.skip=true -Dbaseline.skip=true)
cd "$ROOT"
if [ "${SKIP_OAK_BUILD:-0}" != 1 ]; then
  # fast path when ~/.m2 already holds this version of the upstream modules; otherwise build them too
  mvn "${MVN_FLAGS[@]}" -o install -pl oak-blob-cloud-azure >/dev/null 2>&1 \
    || mvn "${MVN_FLAGS[@]}" install -pl oak-blob-cloud-azure -am
fi
OAK_VERSION=$(sed -n 's/.*<version>\(.*-SNAPSHOT\)<\/version>.*/\1/p' oak-blob-cloud-azure/pom.xml | head -1)
mvn -q -f "$HERE/pom.xml" -Doak.version="$OAK_VERSION" package dependency:build-classpath \
    -Dmdep.outputFile="$HERE/target/classpath.txt"
echo "built oak-blob-cloud-azure $OAK_VERSION and demo client"
