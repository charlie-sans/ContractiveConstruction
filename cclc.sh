#!/bin/bash
# CCL Compiler Launcher Shell Script
# This script sets up the environment and launches the CCL compiler.
# Usage: ./cclc.sh [--lang <language>] [--debug] <source file>

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Path to the CCL jar file (relative to project root)
CCL_JAR_PATH="$SCRIPT_DIR/ccl/target/ccl-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Check if the jar file exists
if [ ! -f "$CCL_JAR_PATH" ]; then
    echo "Error: CCL jar file not found at $CCL_JAR_PATH"
    echo "Please run 'mvn install' to build the project first."
    exit 1
fi

# Launch the compiler
java -jar "$CCL_JAR_PATH" "$@"