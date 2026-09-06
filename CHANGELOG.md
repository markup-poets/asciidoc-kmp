# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.2] - 2026-09-06

### Fixed

- **`include::` now honors the `leveloffset` attribute**, and an included
  document's own title is folded into a section instead of being silently
  dropped (#97). The parser already captured `leveloffset` correctly;
  `DefaultIncludeResolver` never read it. Relative offsets (`+N`/`-N`)
  compound correctly through nested includes; the bare absolute form is
  treated as relative for now (documented limitation, not full Asciidoctor
  "absolute levels aren't context-aware" semantics).
- Docs migrated to Antora, GitHub Pages deploy clobbering fixed, and a native
  (iOS/Linux) TCK fixture-loading issue fixed (#86).

### Changed

- Toolchain/dependency bumps: Kotlin 2.4.10, Gradle wrapper 9.7.1, AGP/KMP
  library plugin 9.3.1, Kotest 6.2.4, `com.gradleup.shadow` 9.6.1,
  `io.github.charlietap.chasm` 1.4.8, `org.eclipse.jgit` 7.7.1, and CI-only
  `actions/setup-java`/`actions/setup-node` bumps.

## [0.1.1] - 2026-07-07

Hotfix: browser support via Kotlin/Wasm.

### Added

- **Kotlin/Wasm browser target** (`wasmJs`) for `asciidoc-parser` and
  `html-renderer`, so the parser and HTML renderer run in the browser as a
  WebAssembly module.
- **`wasm-bridge` module**: browser entry point exporting `convertToHtml()`
  and `version()` to JavaScript via `@JsExport`; builds an ES module +
  `.wasm` pair (`asciidoc-kmp.mjs` / `asciidoc-kmp.wasm`) with
  `:wasm-bridge:wasmJsBrowserDistribution`. Powers the live demo on the
  Markup Poets website.
- `wasmJs` actuals for `PlatformFileReader`/`PlatformFileWriter` in
  `html-renderer` (file I/O is unavailable in the browser; both return
  failed `Result`s — use inline CSS content or built-in themes).

## [0.1.0] - 2026-07-07

First public release. Published to Maven Central under the `org.markup-poet`
namespace for JVM, Android (API 24+), iOS (x64, arm64, simulator arm64),
Linux x64, and macOS arm64.

### Added

- **ASG-native parser** (`asciidoc-parser`, zero dependencies): the Abstract
  Semantic Graph mirroring the official AsciiDoc schema is the only document
  model. Passes all 13 currently published tests of the official AsciiDoc
  Language TCK (commit `3490153d`) via the official Node.js harness —
  compatibility is a work in progress, not an official certification.
- **Syntax coverage**: sections, paragraphs, ordered/unordered/description/
  callout lists with nesting and continuation, tables (`|===`, `cols`
  alignment/widths, colspans, header rows), admonitions, sidebar/example/
  quote/open containers, listing/literal/pass/stem/verse verbatim blocks,
  block macros, breaks, `include::`/`ifdef::`/`ifndef::`/`ifeval::`
  directives, document header with author line, block metadata
  (`[#id.role%option]`, `.Title`), and rich inlines (strong/emphasis/code/
  mark/subscript/superscript spans, links, autolinks, xref shorthand and
  macro forms, attribute references, generic inline macros).
- **Document processing** (`document-processing`): include resolution,
  conditional evaluation, attribute substitution, cross-reference indexing,
  table-of-contents generation and insertion (`:toc:` auto/preamble/macro),
  callout pairing, footnotes, bibliography, validation.
- **HTML renderer** (`html-renderer`): themed HTML output (Default, Dark,
  Kotlin, Minimal themes), CSS variables and custom-CSS seams, custom
  renderer SPI.
- **WASM plugin system** (`plugin-api`, `plugin-engine`,
  `plugin-integration`; Chasm runtime): sandboxed, language-agnostic
  extension plugins with four capability types — custom blocks, block
  macros, inline macros, and converters — plus structured ASG splicing
  (`contentType: "asg"`). ABI v1; Rust example plugin included.
- **Official ASG JSON** (`asciidoc-asg`): serializer and deserializer for
  the official ASG interchange format; TCK adapter (`tck-adapter`).
- **Tooling**: Graphviz DOT export of the ASG (`asg-graphviz-export`),
  AsciiDoc → HTML CLI (`html-cli`, JVM + native), document-processing CLI
  (`cli-app`), multi-file assembly (`antora-resolution`,
  `antora-assembler`).

[0.1.1]: https://github.com/markup-poets/asciidoc-kmp/releases/tag/v0.1.1
[0.1.0]: https://github.com/markup-poets/asciidoc-kmp/releases/tag/v0.1.0
