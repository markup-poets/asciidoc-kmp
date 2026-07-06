#!/usr/bin/env bash
# Sync the official Eclipse AsciiDoc TCK into tck-quality-testing/official-tck/repository.
# The clone is gitignored; this script is the single source of truth for syncing.
set -euo pipefail

REPO_URL="https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck.git"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TCK_DIR="$ROOT_DIR/tck-quality-testing/official-tck/repository"
COMMIT_FILE="$ROOT_DIR/tck-quality-testing/official-tck/commit-hash.txt"

# Pin to a specific commit by exporting ASCIIDOC_TCK_COMMIT; defaults to origin/main.
TARGET_REF="${ASCIIDOC_TCK_COMMIT:-origin/main}"

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

TEST_COUNT="$(find "$TCK_DIR/tests" -name '*-input.adoc' | wc -l | tr -d ' ')"
echo "Synced official TCK to commit $COMMIT ($TEST_COUNT tests)" >&2
