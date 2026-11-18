#!/usr/bin/env bash
# Sync the official Eclipse AsciiDoc TCK into tck-quality-testing/official-tck/repository.
# The clone is gitignored; this script is the single source of truth for syncing,
# and tck-pin.txt (tracked) records which upstream commit is tested.
set -euo pipefail

REPO_URL="https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck.git"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TCK_DIR="$ROOT_DIR/tck-quality-testing/official-tck/repository"
COMMIT_FILE="$ROOT_DIR/tck-quality-testing/official-tck/commit-hash.txt"

PIN_FILE="$ROOT_DIR/tck-quality-testing/official-tck/tck-pin.txt"

# Which TCK commit is tested (the pin) is tracked in tck-pin.txt, so local runs and
# CI test the same, reproducible TCK version.
#   ./scripts/sync-official-tck.sh            check out the pinned commit
#   ./scripts/sync-official-tck.sh --latest   move the pin to upstream HEAD (commit the result)
#   ASCIIDOC_TCK_COMMIT=<ref> ...             one-off override, pin file untouched
UPDATE_PIN=false
if [ "${1:-}" = "--latest" ]; then
  UPDATE_PIN=true
  TARGET_REF="origin/main"
elif [ -n "${ASCIIDOC_TCK_COMMIT:-}" ]; then
  TARGET_REF="$ASCIIDOC_TCK_COMMIT"
else
  TARGET_REF="$(tr -d '[:space:]' < "$PIN_FILE")"
fi

if [ ! -d "$TCK_DIR/.git" ]; then
  echo "Cloning official TCK into $TCK_DIR" >&2
  git clone "$REPO_URL" "$TCK_DIR"
else
  echo "Fetching official TCK updates" >&2
  git -C "$TCK_DIR" fetch origin
fi

git -C "$TCK_DIR" reset --hard "$TARGET_REF"

COMMIT="$(git -C "$TCK_DIR" rev-parse HEAD)"
echo "$COMMIT" > "$COMMIT_FILE"
if [ "$UPDATE_PIN" = true ]; then
  echo "$COMMIT" > "$PIN_FILE"
fi
VERSION="$(git -C "$TCK_DIR" describe --tags --always)"

TEST_COUNT="$(find "$TCK_DIR/tests" -name '*-input.adoc' | wc -l | tr -d ' ')"
echo "Synced official TCK to $VERSION (commit $COMMIT, $TEST_COUNT tests)" >&2
