# Open issues, TCK status, and AsciiDoc Working Group guidance

Status as of 2026-11-17. This file records what was open, what the Eclipse
AsciiDoc Working Group advised, what was changed in response, and what is
still open. Update the checklists as pull requests merge; delete the file
once everything here is closed.

## 1. Guidance received from the AsciiDoc Working Group

In reply to a question on the `asciidoc-lang-dev` mailing list about how to
word the project's compatibility goal and how to report TCK results
(reply received 2026-11-16):

| Topic | Guidance | Where it is applied |
|---|---|---|
| Wording | Say the project "aims to be compatible with" the specification, rather than "aims to implement … in a way that is compatible with". The "work in progress, not an official certification" caveat is right and is the important part. | README, docs compatibility note, TCK status page, `CLAUDE.md` |
| Certification | There is no official certification or compatibility program today. A self-certification process is a Working Group goal for 2027. The specification and TCK are still in progress. | All "certification" wording removed from generated reports; docs state that no program exists |
| Reporting | No official terminology or report format exists yet. Until one does, report: (1) the TCK version tested, as a tag or commit hash; (2) the number of passing and failing tests; (3) the tests skipped or not yet supported, with reasons, and any feature intentionally not supported. | TCK status page, README, published `/tck/` page, generated reports |
| Feedback | Feedback on TCK gaps and ambiguous parts of the specification is welcome, in the AsciiDoc chat (<https://chat.asciidoc.org>) or as issues at <https://gitlab.eclipse.org/eclipse/asciidoc-lang>. | Section 6 below; contributor docs |
| Trademark | "AsciiDoc" is a trademark. Using it to describe what a project processes is common, but naming and compatibility-claim guidelines are being prepared. Watch asciidoc.org and Working Group announcements. | Trademark notice in README and docs; section 7 below |

## 2. State found before the changes

- **TCK drift.** Docs recorded TCK commit `3490153d` with 13 of 13 passing.
  Upstream had moved to `cdfada9c` (`0.1.0-25-gcdfada9`; the only tag is
  `0.1.0`). The 25 commits since the tag changed the harness only (`--version`
  option, `deepStrictEqual` diffs, test loader); the test set is still 13.
- **No real pin.** Docs said the TCK commit was pinned deliberately, but both
  CI workflows synced to upstream HEAD on every run, so a build could change
  under an unchanged commit.
- **Certification claims in code.** The report generators in
  `tck-quality-testing` printed "READY FOR CERTIFICATION" / "NOT READY FOR
  CERTIFICATION" and had a `CertificationChecker`. Generated reports also
  stated a library version and a spec version of `1.0.0`, both hardcoded.
- **Report gaps.** No failing or skipped counts, no list of skipped or
  unsupported tests, no statement of intentionally unsupported features.
- **Wording.** README and the TCK status page used the "implement … in a way
  that is compatible" phrasing; `CLAUDE.md` said "full compatibility"; README
  called the project's own fixture suite a "TCK".
- **No trademark notice** anywhere.
- **Three open issues**: #117, #138, #139 (section 4).

## 3. Current TCK result

| | |
|---|---|
| TCK version tested | `0.1.0-25-gcdfada9` (commit `cdfada9c2768b164eadf4bc12e9f9c68e6caf68a`) |
| Passing | 13 of 13 |
| Failing | 0 |
| Skipped or unsupported tests | none |
| Intentionally unsupported features | none within the scope of the tested TCK |
| How | official Node.js harness, source locations included in the comparison |

One limitation applies to the TCK adapter only: constructs the official ASG
schema does not describe yet (include and conditional directives, tables,
custom block macros, comments, callouts, footnotes) are parsed by the library
but refused by the TCK serialization path. No published TCK test contains
them. The single source for this list is
`docs/modules/ROOT/partials/tck-unsupported.adoc`.

Reproduce: `./scripts/sync-official-tck.sh && ./run-official-tck.sh`.

## 4. Work items

Four branches, stacked in this order so they merge without conflicts.

### 4.1 TCK reporting and Working Group guidance — `fix/tck-reporting-eclipse-guidance`

- [x] Re-sync the TCK to `cdfada9c`; 13 of 13 still pass.
- [x] Pin the tested commit in the tracked file
      `tck-quality-testing/official-tck/tck-pin.txt`.
      `scripts/sync-official-tck.sh` checks out the pin (locally and in CI);
      `--latest` moves the pin to upstream HEAD; `ASCIIDOC_TCK_COMMIT` still
      overrides for a one-off run.
- [x] Report TCK version, passing, failing, skipped, and the unsupported
      lists on the TCK status page, in the README, and on the published
      `/tck/` page. New Antora attributes: `tck-version`, `tck-failing`,
      `tck-skipped`.
- [x] Replace the wording with "aims to be compatible with" everywhere.
- [x] Remove certification language from generated reports:
      `CertificationChecker` → `TckStatusChecker`, "Certification Status" →
      "TCK Status", report title → "AsciiDoc TCK Results Report". Reports now
      show the full TCK commit hash, the real library version, a "Skipped or
      Unsupported Tests" section, and an "Intentionally Unsupported Features"
      section.
- [x] Stop reporting a spec version of `1.0.0`; the specification has no
      released version, so reports say `unreleased`.
- [x] Add the AsciiDoc trademark notice (README, docs home page, TCK status
      page, `/tck/` page).
- [x] Contributor docs: how to move the pin, what every statement of results
      must carry, where to send upstream feedback.

### 4.2 Issue #117, stale artifact names in published metadata — `fix/117-project-version`

- [x] All 13 modules take `version` from `VERSION_NAME` instead of hardcoding
      `0.1.1`.
- [x] Verified: the generated module metadata for `asciidoc-parser` now names
      `asciidoc-parser-jvm-0.1.4.jar` (was `…-0.1.1.jar`).
- Already-published metadata on Maven Central cannot be amended; the fix
  takes effect with the next release.

### 4.3 Issue #139, demo document does not render — `fix/139-demo-document-gaps`

The document from the issue rendered with five macro placeholders and several
missing structures. Fixed, with a regression test per construct
(`DemoDocumentRenderingTest`, 12 tests):

| Construct | Before | Now |
|---|---|---|
| Document title and author line | not rendered in the page body | `<header>` with `<h1>`, author, mailto link |
| `toc::[]` with `:toc: macro` | empty `<div>` | filled with section links |
| `:sectnums:` | ignored | `1.`, `1.1.` prefixes on headings and TOC entries |
| `icon:name[]`, `set=brands` | placeholder | icon name as text (`[github]`) |
| `{attr}` whose value has markup | value shown as literal text | value is parsed as inline markup |
| `video::id[youtube,list=…]` | `<video src="id">`, title dropped | link to the video page, with title |
| `stem:[…\]…]` | truncated at the first `]`, placeholder | full formula source in `<code class="stem">` |
| `latexmath:[…]` | placeholder | formula source, tagged `latexmath` |
| `[stem]` passthrough block | bare text | `stemblock` with the formula source |
| `footnote:id[text]` in a section title | placeholder, id polluted | footnote marker, clean id, footnote list |
| `[%collapsible%open]` example | plain example block | `<details open>` |
| `cols="30%,70%"` | widths ignored | `<colgroup>` with widths |

Official TCK unchanged at 13 of 13 after the parser changes.

Decision (2026-11-18): the core renderer stays neutral and loads nothing
from third parties. Font Awesome icons, MathJax typesetting and embedded
YouTube/Vimeo players are converter conventions for web output, not part of
the AsciiDoc language, so they are provided by WASM extension plugins
(branch `feat/renderer-plugins`) instead of the core.

Not fixed, noted on the way:

- [ ] E-mail addresses in table cells are not auto-linked.
- [ ] Footnote text is rendered as plain text (inline markup inside a
      footnote is not parsed).
- [ ] `{url-attribute}[link text]` links the URL but leaves `[link text]`
      as literal text, because attribute values are substituted per
      reference rather than before inline parsing.
- [ ] The generated TOC markup nests an extra `<ul>` when the first heading
      is level 2 (existed before this work).
- [ ] The `html-cli` does not run the document-processing phase, so features
      implemented only there (auto-placed `:toc:`, bibliography, validation)
      are not available from the command line.

### 4.5 Presentation features as WASM plugins — `feat/renderer-plugins`

Follows the decision in 4.3: the core renders icons, formulas and
YouTube/Vimeo videos neutrally; the web conventions are plugins.

- [x] Three standard plugins written in Kotlin/Wasm (`plugins/`): `icons`
      (Font Awesome), `math` (MathJax), `video` (YouTube/Vimeo players).
      Off by default; `html-renderer --with icons,math,video` or
      `StandardPlugin.*.wasm()` from the new `standard-plugins` module.
- [x] `plugin-sdk` (guest side of the ABI for Kotlin plugins); `plugin-api`
      builds for `wasmWasi`.
- [x] ABI additions, still v1: `backend` in the request, block `id` / `role` /
      `options` / `title` in the attributes, `assets` in the response,
      claiming built-in names, declining a construct.
- [x] Engine: inert WASI stubs so Kotlin/Wasm modules load; any other import
      is still refused. Verified on Chasm 2.1.0.
- [x] `OutputOptions.headContent` in the renderer (the only core change).
- [x] Android target for the plugin modules.
- Measured (JVM 17, this machine): each plugin is about 220 KB and loads in
  40 to 65 ms; converting the #139 demo document takes about 9 ms with all
  three plugins against 0.2 ms core-only, because every claimed construct is
  a JSON round trip through an interpreter.
- [x] Browser (`feat/browser-plugin-dispatcher`): `BrowserPluginEngine` runs
      plugins on the browser's own WebAssembly engine; `wasm-bridge` fetches
      the standard plugins on demand from `plugins/` next to the bundle.
      Bundle size 303 KB → 481 KB (the plugin pass and JSON handling); the
      three plugin files are about 220 KB each and only downloaded when
      enabled. Tested in headless Chromium only.
- [x] The website that hosts the live demo (separate repository) deploys the
      `plugins/` directory with the bundle and places the returned `head`
      markup; prepared on its `feat/demo-plugins` branch.
- [x] MathJax configuration checked in Chromium with MathJax 3.2.2 through
      the live demo. It found one defect: the TeX input's escape handling
      turned the `\$` AsciiMath delimiters into literal `$` signs around
      every AsciiMath formula. The configuration now sets
      `processEscapes: false`.
- [ ] The parser does not recognise the revision line (`v1.0, 2026`) under
      the author line; it ends the header, so attribute entries after it are
      not document attributes.
- [ ] Plugin calls are one per construct; batching per paragraph would cut
      the cost for icon-heavy documents.
- [ ] `converter` plugins still see top-level blocks only.

### 4.4 Issue #138, performance testing — `feat/138-performance-harness`

- [x] `./gradlew :tck-quality-testing:perfReport` measures parse and
      parse + HTML render time (median, p95, MB/s) on the JVM over the TCK
      inputs, a kitchen-sink document, and generated documents of 40, 250 and
      1000 sections; writes `perf-report.md` and `perf-report.json`.
- [x] The Docs workflow publishes the table on the `/tck/` page.
      Informational, not a CI gate.
- First local measurement (JVM 17, Linux amd64, 12 cores, version 0.1.4):
  about 18 MB/s parsing and 14.5 MB/s parsing plus rendering on the generated
  documents (690 KB in 39 ms and 48 ms).
- [ ] No comparison with other processors (the issue links acdc's published
      numbers). A fair comparison needs both tools on the same machine and
      corpus; not done.
- [ ] JVM only. Native targets are not measured.

## 5. Not done, needs the maintainer

- [ ] Reply to the Working Group thread (a thank-you and a link to the new
      status page is enough).
- [ ] Comment on and close #117, #138, #139 once the pull requests merge.
- [ ] Nine open Dependabot pull requests against `develop` (#122–#137) were
      left untouched.
- [ ] `git config user.email` is not set on the development machine; commits
      for this work were made with the address used by earlier commits.

## 6. Feedback for upstream

Collected while working against the TCK; nothing has been sent yet.

- **TCK version identity.** The only tag is `0.1.0` and the harness has
  changed since. Results can only be identified by commit hash or
  `git describe` output. A tag per published test set would make results
  comparable across implementations.
- **Coverage.** The 13 published tests are 11 block tests (document, header,
  paragraph, section, listing, sidebar, one unordered list) and 2 inline
  tests (no markup, strong span). There are none for other inline markup,
  ordered or description lists, tables, attributes, macros, or directives,
  so "13 of 13" says little about practical compatibility.
- **ASG schema scope.** The schema has no shape for include and conditional
  directives, tables, footnotes, or callouts. Implementations have to invent
  nodes for these, which then cannot be exercised by the TCK.
- **Attribute references and locations.** When an attribute value contains
  inline markup, the specification does not say what source location the
  resulting nodes carry. This project gives them the location of the
  `{reference}`.

## 7. Things to watch

- Naming and compatibility-claim guidelines for the AsciiDoc trademark
  (asciidoc.org, Working Group announcements). The repository name
  `asciidoc-kmp` and the phrase "AsciiDoc converter" may need review when
  they are published.
- The planned self-certification process (Working Group goal for 2027). The
  report format in section 3 follows the interim advice so results stay
  comparable once it exists.
- New TCK tags or tests: run `./scripts/sync-official-tck.sh --latest`, then
  follow "When results change" in the TCK workflow page.
