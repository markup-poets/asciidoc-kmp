//! Example Markup Poet WASM plugin implementing plugin ABI v1.
//!
//! Registers a custom `[shout]` block processor that uppercases the block's
//! content and renders it as HTML.
//!
//! ABI (see docs/PLUGINS.md in the Markup Poet repository):
//! - exports: memory, plugin_alloc, plugin_dealloc, plugin_info, process,
//!   and optionally on_load / on_unload
//! - all payloads are 4-byte little-endian length-prefixed UTF-8 JSON in
//!   linear memory

use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;

// ---------------------------------------------------------------------------
// Memory management
// ---------------------------------------------------------------------------

#[no_mangle]
pub extern "C" fn plugin_alloc(size: u32) -> *mut u8 {
    let mut buf = Vec::<u8>::with_capacity(size as usize);
    let ptr = buf.as_mut_ptr();
    std::mem::forget(buf);
    ptr
}

/// # Safety
/// `ptr` must have been returned by `plugin_alloc(size)` and not freed since.
#[no_mangle]
pub unsafe extern "C" fn plugin_dealloc(ptr: *mut u8, size: u32) {
    drop(Vec::from_raw_parts(ptr, 0, size as usize));
}

/// Copies `payload` into a fresh length-prefixed buffer and returns its pointer.
fn to_length_prefixed(payload: &[u8]) -> *const u8 {
    let mut buf = Vec::with_capacity(4 + payload.len());
    buf.extend_from_slice(&(payload.len() as u32).to_le_bytes());
    buf.extend_from_slice(payload);
    let ptr = buf.as_ptr();
    std::mem::forget(buf);
    ptr
}

/// # Safety
/// `ptr`/`len` must describe a valid, initialized region of linear memory.
unsafe fn from_raw(ptr: *const u8, len: u32) -> &'static [u8] {
    std::slice::from_raw_parts(ptr, len as usize)
}

// ---------------------------------------------------------------------------
// ABI payloads
// ---------------------------------------------------------------------------

#[derive(Serialize)]
struct Capability {
    #[serde(rename = "type")]
    kind: String,
    name: String,
}

#[derive(Serialize)]
struct Descriptor {
    #[serde(rename = "abiVersion")]
    abi_version: u32,
    id: String,
    name: String,
    version: String,
    description: String,
    capabilities: Vec<Capability>,
    metadata: BTreeMap<String, String>,
}

#[derive(Deserialize)]
struct Invocation {
    #[serde(rename = "abiVersion")]
    #[allow(dead_code)]
    abi_version: u32,
    #[serde(rename = "extensionPoint")]
    extension_point: String,
    name: String,
    #[serde(default)]
    content: String,
}

#[derive(Serialize)]
struct Replacement {
    #[serde(rename = "contentType")]
    content_type: String,
    value: String,
}

#[derive(Serialize)]
struct Response {
    ok: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    replacement: Option<Replacement>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<String>,
    warnings: Vec<String>,
}

// ---------------------------------------------------------------------------
// Exports
// ---------------------------------------------------------------------------

#[no_mangle]
pub extern "C" fn plugin_info() -> *const u8 {
    let descriptor = Descriptor {
        abi_version: 1,
        id: "shout-plugin".into(),
        name: "Shout".into(),
        version: "0.1.0".into(),
        description: "Uppercases the content of [shout] blocks".into(),
        capabilities: vec![Capability {
            kind: "block".into(),
            name: "shout".into(),
        }],
        metadata: BTreeMap::new(),
    };
    to_length_prefixed(serde_json::to_vec(&descriptor).unwrap().as_slice())
}

/// # Safety
/// `ptr`/`len` must describe a valid invocation payload written by the host.
#[no_mangle]
pub unsafe extern "C" fn process(ptr: *const u8, len: u32) -> *const u8 {
    let response = match serde_json::from_slice::<Invocation>(from_raw(ptr, len)) {
        Ok(invocation) if invocation.extension_point == "block" && invocation.name == "shout" => {
            let escaped = invocation
                .content
                .to_uppercase()
                .replace('&', "&amp;")
                .replace('<', "&lt;")
                .replace('>', "&gt;");
            Response {
                ok: true,
                replacement: Some(Replacement {
                    content_type: "html".into(),
                    value: format!("<div class=\"shout\">{}!</div>", escaped),
                }),
                error: None,
                warnings: vec![],
            }
        }
        Ok(invocation) => Response {
            ok: false,
            replacement: None,
            error: Some(format!(
                "unsupported capability: {} {}",
                invocation.extension_point, invocation.name
            )),
            warnings: vec![],
        },
        Err(e) => Response {
            ok: false,
            replacement: None,
            error: Some(format!("invalid invocation payload: {e}")),
            warnings: vec![],
        },
    };
    to_length_prefixed(serde_json::to_vec(&response).unwrap().as_slice())
}

#[no_mangle]
pub extern "C" fn on_load() {}

#[no_mangle]
pub extern "C" fn on_unload() {}
