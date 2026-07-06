# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[0.1.0]: https://github.com/markup-poets/asciidoc-kmp/releases/tag/v0.1.0
