#!/usr/bin/env bash
# Builds the shout example plugin to WASM and refreshes the plugin-engine
# test fixture. Requires rustup with the wasm32-unknown-unknown target:
#   rustup target add wasm32-unknown-unknown
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$DIR/../../.." && pwd)"

if ! command -v cargo >/dev/null 2>&1 && [ -x "$HOME/.cargo/bin/cargo" ]; then
  PATH="$HOME/.cargo/bin:$PATH"
fi

cargo build --release --target wasm32-unknown-unknown --manifest-path "$DIR/Cargo.toml"

WASM="$DIR/target/wasm32-unknown-unknown/release/shout_plugin.wasm"
FIXTURE="$ROOT_DIR/plugin-engine/src/jvmTest/resources/fixtures/shout.wasm"
cp "$WASM" "$FIXTURE"
echo "Built $WASM"
echo "Refreshed fixture $FIXTURE"
