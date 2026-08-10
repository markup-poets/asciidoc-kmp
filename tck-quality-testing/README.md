# tck-quality-testing

Fixture-replay harness, TCK sync, and conformance reporting.

Two harnesses live here, and the distinction matters:

- **`./run-official-tck.sh`** — the official Eclipse TCK via its Node.js harness. **This is the conformance gate**; a parser change must never reduce its pass count.
- **`./gradlew :tck-quality-testing:jvmTest`** — fast in-process fixture replay, for the inner loop. Convenient, but not the gate.

The upstream TCK clone under `official-tck/repository/` is gitignored and synced by `scripts/sync-official-tck.sh`. This module's own fixtures are in `fixtures/`.

**Full documentation:**

- [TCK workflow](https://markup-poets.github.io/asciidoc-kmp/contributing/tck-workflow.html) — the contributor loop, adapter rules, adding fixtures
- [TCK architecture and data flow](https://markup-poets.github.io/asciidoc-kmp/explanation/tck-architecture.html) — how sync, execution, and reporting fit together
- [Official TCK fixture format](https://markup-poets.github.io/asciidoc-kmp/reference/official-tck-fixture-format.html) — the upstream paired-file layout
- [TCK status and syntax coverage](https://markup-poets.github.io/asciidoc-kmp/reference/tck-and-syntax-coverage.html) — current pass count
