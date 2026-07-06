# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Markup Poet is a Kotlin Multiplatform AsciiDoc converter targeting JVM, Android (API 24+), iOS (x64, ARM64, Simulator ARM64), Linux (x64), and macOS (ARM64). The core parser has zero external dependencies. The project's goal is full compatibility with the official Eclipse AsciiDoc TCK, verified through the official Node.js harness.

## Modules

- `asciidoc-parser` — core parser + ASG document model (`org.markup.poet.asciidoc.{parser,asg,error}`); zero deps
- `asciidoc-asg` — serializes the parsed document to the official ASG JSON format (`org.markup.poet.asciidoc.asg`)
- `tck-adapter` — CLI adapter for the official TCK harness (stdin JSON request → ASG JSON on stdout)
- `tck-quality-testing` — fixture-replay harness, TCK sync, conformance reporting (`org.markup.poet.tck`)
- `document-processing` — post-parse phase: includes, attribute substitution, conditionals, TOC, cross-refs
- `html-renderer` — ASG → HTML with theming (`org.markup.poet.asciidoc.render`)
- `html-cli` — AsciiDoc → HTML command line tool (JVM + native)
- `cli-app` — AsciiDoc → Graphviz DOT export CLI
- `asg-graphviz-export` — ASG → DOT conversion library
- `antora-resolution`, `antora-assembler` — multi-file document assembly
- `plugin-api`, `plugin-engine`, `plugin-integration` — WASM extension plugins (Chasm runtime; see docs/PLUGINS.md); example plugin in `examples/plugins/shout-rust/`

## Build Commands

```bash
./gradlew build                                # Full build
./gradlew :asciidoc-parser:jvmTest             # Parser JVM tests
./gradlew :tck-quality-testing:jvmTest         # Fixture-replay inner loop (fast)
./gradlew :tck-adapter:officialTck             # OFFICIAL TCK harness run (source of truth)
./run-official-tck.sh                          # Same as officialTck, directly
./scripts/sync-official-tck.sh                 # Sync official TCK repo to upstream HEAD
```

The official TCK harness requires Node.js >= 20. The Java toolchain is pinned to 17 via `jvmToolchain(17)` in every module.

## TCK Compatibility (the core goal)

The official Eclipse AsciiDoc TCK (`tck-quality-testing/official-tck/repository/`, gitignored, synced via script) spawns `tck-adapter` once per test, writes `{"contents", "path", "type"}` JSON to stdin, and compares the ASG JSON printed to stdout against the expected output. Rules learned from the harness/fixtures:

- stdout must contain ONLY the ASG JSON; all logging goes to stderr
- ASG locations are 1-based with END-INCLUSIVE columns; if the output contains no `location` keys at all, the harness compares with locations stripped (escape hatch while positions are incomplete; toggle with `TCK_ADAPTER_LOCATIONS=true`)
- multi-line plain text is ONE text node with `\n` in its value
- lists use `variant` + `marker`, items are `listItem` nodes with `principal`
- fixture key order varies — compare structurally, never string-compare

## Architecture

Pipeline: Parse (`asciidoc-parser`) → Process (`document-processing`) → Render (`html-renderer`). Every phase operates on the ASG model (`org.markup.poet.asciidoc.asg`), which mirrors the official schema: node axes `name`/`variant`/`form` are fields, so new syntax mostly means parser work, not new node classes.

Parser: `DefaultAsciidocParser` facade (`parse(source): ParseResult`) → `BlockTreeParser`/`AsgInlineParser` (`parser/asg/`) → `AsgDocument` (sealed `Block`/`Inline` hierarchies). `AsgProcessingNodes.kt` adds non-schema extension nodes: the parser core emits includes, conditionals, custom block macros, and tables for the corresponding syntax; the rest (comments, callouts, footnotes, citations, raw pre-rendered output) are injected/consumed by document-processing and extension plugins. The TCK serialization path refuses all of them (no TCK fixture contains these constructs).

## Testing

Tests use kotlin-test and Kotest (property-based, `checkAll` with 100+ iterations). The official TCK harness (`:tck-adapter:officialTck`) is the conformance gate: parser changes must never reduce the official pass count.

## Specifications

Current design documentation lives in `docs/`. (Requirement numbers cited in some tck-quality-testing test comments refer to the project's original spec-driven design documents, which were removed; git history has them if ever needed.) The official ASG schema is in the asciidoc-lang repo (`asg/schema.json`); the synced TCK fixtures under `tck-quality-testing/official-tck/repository/tests/` are the ground truth when prose is ambiguous.
