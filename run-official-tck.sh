#!/usr/bin/env bash
# Run the OFFICIAL Eclipse AsciiDoc TCK harness against this implementation.
# This is the source of truth for conformance; the Gradle jvmTest fixture replay
# is only a fast inner development loop.
#
# Usage: ./run-official-tck.sh
# Env:   TCK_ADAPTER_LOCATIONS=false   omit source locations (compared leniently)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
TCK_DIR="$ROOT_DIR/tck-quality-testing/official-tck/repository"
ADAPTER_JAR="$ROOT_DIR/tck-adapter/build/libs/tck-adapter-1.0.0-all.jar"

# Prefer a user-local node install if node is not already on the PATH.
if ! command -v node >/dev/null 2>&1; then
  for d in "$HOME"/.local/opt/node-*/bin; do
    [ -d "$d" ] && PATH="$d:$PATH" && break
  done
fi
if ! command -v node >/dev/null 2>&1; then
  echo "ERROR: Node.js >= 20 is required for the official TCK harness." >&2
  echo "Install it from https://nodejs.org or via your package manager / nvm." >&2
  exit 1
fi
NODE_MAJOR="$(node --version | sed 's/^v\([0-9]*\).*/\1/')"
if [ "$NODE_MAJOR" -lt 20 ]; then
  echo "ERROR: Node.js >= 20 required, found $(node --version)." >&2
  exit 1
fi

if [ ! -d "$TCK_DIR/tests" ]; then
  "$ROOT_DIR/scripts/sync-official-tck.sh"
fi
if [ ! -d "$TCK_DIR/harness/node_modules" ]; then
  (cd "$TCK_DIR/harness" && npm ci --no-audit --no-fund)
fi

echo "Building TCK adapter..." >&2
(cd "$ROOT_DIR" && ./gradlew -q :tck-adapter:shadowJar)

echo "Running official AsciiDoc TCK (commit $(git -C "$TCK_DIR" rev-parse --short HEAD))..." >&2
cd "$TCK_DIR"
exec node harness/bin/asciidoc-tck.js cli \
  --adapter-command "env TCK_ADAPTER_LOCATIONS=${TCK_ADAPTER_LOCATIONS:-true} java -jar $ADAPTER_JAR"
