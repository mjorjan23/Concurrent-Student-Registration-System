#!/usr/bin/env bash

# Runs the Java Assignment 3 asynchronous event-driven workflow.
# Usage: ./run_java_async.sh [config-file] [mode]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

CONFIG_FILE="${1:-config/normal.properties}"
MODE="${2:-}"
BUILD_DIR="out/async_run"

if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
    echo "Error: Java JDK is required (javac and java must be available)." >&2
    exit 1
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "Error: Config file not found: $CONFIG_FILE" >&2
    exit 1
fi

mkdir -p "$BUILD_DIR"
echo "Compiling Java source files..."
javac -d "$BUILD_DIR" src/*.java

echo "Running Java async workflow with $CONFIG_FILE${MODE:+ in mode $MODE}..."
if [[ -n "$MODE" ]]; then
    java -cp "$BUILD_DIR" AsyncMain "$CONFIG_FILE" "$MODE"
else
    java -cp "$BUILD_DIR" AsyncMain "$CONFIG_FILE"
fi
