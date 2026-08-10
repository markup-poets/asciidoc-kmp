# antora-assembler

Assembles a multi-file Antora document tree into a single consolidated AsciiDoc file — resolving `include::[]` directives recursively and Antora resource coordinates (`partial$`, `example$`, `page$`, `image$`), detecting circular dependencies, and merging document attributes.

```bash
./assemble-article.sh my-article/index.adoc my-article/assembled.adoc
```

**Full documentation:** [Assemble multi-file documents](https://markup-poets.github.io/asciidoc-kmp/how-to/assemble-multi-file-documents.html) — the wrapper script, the CLI and its options, and the programmatic API.
