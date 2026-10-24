#!/usr/bin/env bash
# Store one large binary through Oak's AzureDataStore (v8 backend) against Azurite with a bounded heap.
#   tools/demo/upload.sh            # 2 GB binary, 1 GB heap
#   tools/demo/upload.sh 512m       # other binary size
#   HEAP=2g tools/demo/upload.sh    # other heap size
#   SKIP_BUILD=1 tools/demo/upload.sh
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
SIZE=${1:-2g}
HEAP=${HEAP:-1g}
"$HERE/azurite.sh" start
[ "${SKIP_BUILD:-0}" = 1 ] || "$HERE/build.sh"
exec java -Xmx"$HEAP" -Xms"$HEAP" -XX:+UseG1GC \
  -cp "$HERE/target/classes:$(cat "$HERE/target/classpath.txt")" \
  org.apache.jackrabbit.oak.demo.LargeUpload --size "$SIZE" "${@:2}"
