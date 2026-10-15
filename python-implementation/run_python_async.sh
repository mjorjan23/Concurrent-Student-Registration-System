#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

CONFIG="${1:-config/a4_success.properties}"
if [ "$#" -ge 2 ]; then
  PYTHONPATH=src python3 src/async_main.py "$CONFIG" "$2"
else
  PYTHONPATH=src python3 src/async_main.py "$CONFIG"
fi
