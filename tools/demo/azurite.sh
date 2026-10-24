#!/usr/bin/env bash
# Start / stop / reset the Azurite blob emulator used by the demo (same image as AzuriteDockerRule).
set -euo pipefail
NAME=oak-demo-azurite
IMAGE=mcr.microsoft.com/azure-storage/azurite:3.29.0
PORT=${AZURITE_PORT:-10000}
case "${1:-start}" in
  start)
    if docker ps --format '{{.Names}}' | grep -qx "$NAME"; then
      echo "azurite already running on port $PORT"; exit 0
    fi
    docker rm -f "$NAME" >/dev/null 2>&1 || true
    docker run -d --name "$NAME" -p "$PORT:10000" "$IMAGE" \
      azurite-blob --blobHost 0.0.0.0 --skipApiVersionCheck --silent >/dev/null
    for _ in $(seq 1 30); do
      if curl -s -o /dev/null "http://127.0.0.1:$PORT/devstoreaccount1?comp=list" ; then echo "azurite up on port $PORT"; exit 0; fi
      sleep 0.5
    done
    echo "azurite did not come up"; docker logs "$NAME"; exit 1 ;;
  stop|reset)
    docker rm -f "$NAME" >/dev/null 2>&1 || true
    echo "azurite stopped (all emulator data discarded)" ;;
  logs) docker logs "$NAME" ;;
  *) echo "usage: $0 start|stop|reset|logs"; exit 2 ;;
esac
