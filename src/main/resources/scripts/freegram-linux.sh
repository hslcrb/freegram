#!/bin/bash
# FreeGram Launcher for Linux
# Requires Java 17+ and JavaFX 17+
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/freegram-1.0.0.jar"

JAVAFX_PATH="${JAVAFX_PATH:-/usr/share/openjfx/lib}"
if [ ! -d "$JAVAFX_PATH" ]; then
    JAVAFX_PATH="/usr/lib/jvm/java-17-openjfx/lib"
fi

if [ -d "$JAVAFX_PATH" ]; then
    java --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.web,javafx.swing -jar "$JAR" "$@"
else
    echo "Warning: JavaFX not found at $JAVAFX_PATH"
    echo "Set JAVAFX_PATH environment variable or install openjfx"
    java -jar "$JAR" "$@"
fi
