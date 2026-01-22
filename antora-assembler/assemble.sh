#!/bin/bash
# Convenience script for running the Antora Document Assembler CLI

# Build the JAR if it doesn't exist
JAR_PATH="build/libs/antora-assembler-jvm-1.0.0.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "Building JAR..."
    ../gradlew :antora-assembler:jvmJar
fi

# Run the CLI
java -cp "$JAR_PATH" org.markup.poet.antora.assembler.cli.MainKt "$@"
