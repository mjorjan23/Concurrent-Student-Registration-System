#!/usr/bin/env bash

# Runs the Java Assignment 2-style workflow.
# Usage: ./run_java_a2.sh [config-file]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

CONFIG_FILE="${1:-config/normal.properties}"
BUILD_DIR="out/a2_run"

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

echo "Running Java A2-style workflow with $CONFIG_FILE..."
java -cp "$BUILD_DIR" Main "$CONFIG_FILE"
