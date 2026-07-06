# Markup Poet WASM Plugin System

Markup Poet supports custom AsciiDoc extensions as **sandboxed WebAssembly
plugins**. Where Asciidoctor extensions are Ruby code running with full host
privileges, Markup Poet plugins are language-agnostic WASM modules executed by
[Chasm](https://github.com/CharlieTap/chasm) — a pure-Kotlin WASM runtime —
so they work on every Kotlin Multiplatform target (JVM, Android, iOS, Linux,
macOS) with no native bindings and no ambient authority: a plugin sees only
the JSON envelopes the host hands it. No filesystem, no network, no clock.

## Extension points

| Type          | AsciiDoc syntax                     | Status |
|---------------|-------------------------------------|--------|
| `block`       | `[name]` + delimited/paragraph body | v1     |
| `inlineMacro` | `name:target[attrs]`                | v1     |
| `blockMacro`  | `name::target[attrs]`               | v1     |
| `converter`   | ASG node → HTML string              | v1     |

All four are ABI v1: the capability types and the `asg` content type are
additive — plugins built against the original v1 surface keep working
unchanged.

- **`block`** claims a block whose style (`[name]` attribute line) matches the
  capability name and replaces it *before* rendering.
- **`blockMacro`** claims a `name::target[attrs]` line whose macro name is not
  built in (`image`, `audio`, `video`, `toc`) and not a processing directive
  (`include`, `ifdef`, `ifndef`, `ifeval`, `endif`), and replaces it before
  rendering. An unclaimed custom block macro renders as nothing plus a warning.
- **`inlineMacro`** claims `name:target[attrs]` inline macros.
- **`converter`** does not transform the document — it renders. The capability
  `name` names the block style (e.g. `gallery`) or ASG node kind (e.g.
  `ListBlock`) it renders; the host registers it as a custom renderer and calls
  it whenever the HTML renderer reaches a matching node.

## ABI v1

A plugin is a WASM module (no imports allowed — modules requiring WASI or host
functions are rejected at load) exporting:

| Export | Signature | Purpose |
|---|---|---|
| `memory` | linear memory | shared data buffer |
| `plugin_alloc` | `(i32 size) -> i32 ptr` | host asks the plugin to reserve memory |
| `plugin_dealloc` | `(i32 ptr, i32 size)` | host returns memory to the plugin |
| `plugin_info` | `() -> i32 ptr` | returns the descriptor payload |
| `process` | `(i32 ptr, i32 len) -> i32 ptr` | handles one invocation; `0` = internal failure |
| `on_load` | `()` | optional init hook |
| `on_unload` | `()` | optional shutdown hook |

**Payload protocol**: every payload crossing the boundary is a 4-byte
little-endian length prefix followed by UTF-8 JSON. Payloads are capped at
1 MB (64 KB for the descriptor). The host writes invocations via
`plugin_alloc` + memory writes, calls `process(ptr, len)`, reads the
length-prefixed response at the returned pointer, then returns both buffers
via `plugin_dealloc`.

### Descriptor (`plugin_info`)

```json
{
  "abiVersion": 1,
  "id": "shout-plugin",
  "name": "Shout",
  "version": "0.1.0",
  "description": "Uppercases the content of [shout] blocks",
  "capabilities": [ { "type": "block", "name": "shout" } ],
  "metadata": {}
}
```

Hosts refuse `abiVersion != 1`, duplicate plugin ids, and duplicate
`(type, name)` capability claims.

### Invocation (host → plugin)

```json
{
  "abiVersion": 1,
  "extensionPoint": "block",
  "name": "shout",
  "attributes": { "1": "shout" },
  "content": "raw block body or macro target",
  "documentAttributes": { "toc": "" },
  "location": { "line": 12, "column": 1 }
}
```

`attributes` carries the construct's own attributes: positional attributes
keyed by their 1-based index plus the named attributes. For `blockMacro` and
`inlineMacro` invocations the macro target is additionally available as
`attributes["target"]`, and `content` is the target:

```json
{
  "abiVersion": 1,
  "extensionPoint": "blockMacro",
  "name": "gallery",
  "attributes": { "target": "photos/2024", "1": "grid", "size": "big" },
  "content": "photos/2024",
  "documentAttributes": {},
  "location": { "line": 3, "column": 1 }
}
```

For `converter` invocations `content` is the claimed node serialized as
**official ASG node JSON** (see the converter section below).

### Response (plugin → host)

```json
{
  "ok": true,
  "replacement": { "contentType": "html", "value": "<div class=\"shout\">…</div>" },
  "warnings": []
}
```

- `contentType: "asciidoc"` — the host re-parses the value and splices the
  resulting blocks (attribute substitution and macro expansion still apply).
- `contentType: "html"` — spliced as a raw passthrough block.
- `contentType: "asg"` — official ASG node JSON: `nodes` holds an array of
  nodes (a single node object is also accepted); hosts fall back to parsing
  `value` as JSON when `nodes` is absent. In block context the nodes must be
  blocks, in inline context inlines — a mismatch (or malformed node JSON) is
  treated like `ok: false`: the original construct stays and a warning is
  reported.

  ```json
  {
    "ok": true,
    "replacement": {
      "contentType": "asg",
      "nodes": [
        { "name": "paragraph", "type": "block",
          "inlines": [ { "name": "text", "type": "string", "value": "spliced" } ] }
      ]
    }
  }
  ```

- `ok: false` + `error` — the host leaves the original construct untouched
  and surfaces the error as a processing warning.

## Converter plugins

A `converter` capability plugs into HTML rendering instead of document
processing. The host (e.g. `html-cli --plugin`) registers each converter
capability as a custom renderer under its capability name; the html-renderer
dispatches to it for leaf blocks carrying that block style, or for nodes whose
class simple name matches (e.g. `ListBlock`, `SectionBlock`) — style wins.

The invocation's `content` is the claimed node as official ASG node JSON (the
node-level encoding of `asciidoc-asg`'s `AsgDocumentJsonSerializer`), and
`documentAttributes` carries the resolved document attributes:

```json
{
  "abiVersion": 1,
  "extensionPoint": "converter",
  "name": "gallery",
  "attributes": {},
  "content": "{\"name\":\"listing\",\"type\":\"block\",\"form\":\"delimited\",…}",
  "documentAttributes": { "brand": "poet" },
  "location": { "line": 5, "column": 1 }
}
```

The response must be `{ "contentType": "html", "value": "…" }`; the value is
emitted verbatim in the node's place. Any failure — `ok: false`, another
content type, or a node with no official ASG form — renders as nothing plus a
rendering warning.

## Writing a plugin in Rust

See the complete example in [`examples/plugins/shout-rust/`](../examples/plugins/shout-rust/):
a `cdylib` crate targeting `wasm32-unknown-unknown` with `serde_json` for the
envelopes. Build with `./build.sh` (requires
`rustup target add wasm32-unknown-unknown`).

The essential pattern:

```rust
#[no_mangle]
pub extern "C" fn plugin_alloc(size: u32) -> *mut u8 { /* Vec + mem::forget */ }

#[no_mangle]
pub unsafe extern "C" fn plugin_dealloc(ptr: *mut u8, size: u32) { /* Vec::from_raw_parts */ }

#[no_mangle]
pub extern "C" fn plugin_info() -> *const u8 { /* length-prefixed descriptor JSON */ }

#[no_mangle]
pub unsafe extern "C" fn process(ptr: *const u8, len: u32) -> *const u8 {
    // parse invocation JSON  →  build response JSON  →  length-prefixed buffer
}
```

## Host-side API

```kotlin
val engine = PluginEngine()                       // org.markup.poet.plugin.engine
val plugin = engine.loadPlugin(wasmBytes, "shout.wasm")
val response = plugin.process(
    PluginInvocation(extensionPoint = "block", name = "shout", content = "hello"),
)
engine.forCapability("block", "shout")            // dispatch lookup
engine.unloadAll()
```

The integration layer (`org.markup.poet.plugin.integration`) applies loaded
plugins to a parsed document and wires converters into rendering:

```kotlin
val processed = WasmExtensions(engine).apply(parsedDocument)   // block / blockMacro / inlineMacro
val renderers = converterRenderers(engine, processed.document.attributes)
val config = RenderConfig(customRenderers = renderers)         // converter capabilities
// render with `config`, then engine.unloadAll()
```

`PluginEngine` implements the `PluginDispatch` interface (and `WasmPlugin`
implements `PluginHandle`, both in `org.markup.poet.plugin.api`), so test
suites can drive the integration layer with pure-Kotlin plugin doubles.

`PluginEngine(PluginLimits(...))` customizes payload caps. A plugin whose
invocation fails is marked *poisoned* and refuses further calls — unload it.

## Safety model (v1)

- **No imports**: instantiation fails for modules that require WASI or host
  functions; plugins are pure compute over the envelope.
- **Payload caps**: 1 MB per payload, 64 KB descriptor.
- **Isolation**: one store/instance/memory per plugin; nothing is shared.
- **Poisoning**: a failed invocation permanently disables the instance.
- Known limitation: Chasm has no fuel metering yet, so a hostile infinite
  loop can pin the calling thread. Load only plugins you trust, or invoke
  with a timeout on a background dispatcher.
