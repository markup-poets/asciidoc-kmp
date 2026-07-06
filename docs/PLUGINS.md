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
| `blockMacro`  | `name::target[attrs]`               | planned |
| `converter`   | ASG node → output string            | planned |

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
- `contentType: "asg"` — reserved: ASG node JSON (see `asciidoc-asg`).
- `ok: false` + `error` — the host leaves the original construct untouched
  and surfaces the error as a processing warning.

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
