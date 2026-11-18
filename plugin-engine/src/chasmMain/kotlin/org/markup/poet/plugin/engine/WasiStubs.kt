package org.markup.poet.plugin.engine

import io.github.charlietap.chasm.embedding.dsl.imports
import io.github.charlietap.chasm.embedding.shapes.Import
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.host.writeI32

/**
 * Inert `wasi_snapshot_preview1` imports.
 *
 * Toolchains that target WASI (Kotlin/Wasm among them) emit modules that
 * import a handful of WASI functions even when the plugin code never does
 * I/O. These stubs let such modules instantiate while keeping the sandbox
 * closed: none of them reaches a file, the network, the clock, the
 * environment or a source of randomness. Each returns a fixed WASI errno and
 * touches no memory.
 */
internal object WasiStubs {

    const val MODULE = "wasi_snapshot_preview1"

    private const val SUCCESS = 0
    private const val EBADF = 8
    private const val ENOSYS = 52

    private const val I32 = 'i'
    private const val I64 = 'I'

    private class Stub(val params: String, val errno: Int?)

    /** name → parameter types (`i` = i32, `I` = i64) and the errno returned (null = no result). */
    private val stubs: Map<String, Stub> = mapOf(
        // Output is discarded; reporting success keeps a stray println from failing the plugin.
        "fd_write" to Stub("iiii", SUCCESS),
        "fd_read" to Stub("iiii", EBADF),
        "fd_close" to Stub("i", SUCCESS),
        "fd_seek" to Stub("iIii", EBADF),
        "fd_fdstat_get" to Stub("ii", EBADF),
        "fd_prestat_get" to Stub("ii", EBADF),
        "fd_prestat_dir_name" to Stub("iii", EBADF),
        "environ_sizes_get" to Stub("ii", SUCCESS),
        "environ_get" to Stub("ii", SUCCESS),
        "args_sizes_get" to Stub("ii", SUCCESS),
        "args_get" to Stub("ii", SUCCESS),
        "random_get" to Stub("ii", ENOSYS),
        "clock_time_get" to Stub("iIi", ENOSYS),
        "proc_exit" to Stub("i", null),
    )

    /** True when [moduleName]/[entityName] is an import these stubs can satisfy. */
    fun provides(moduleName: String, entityName: String): Boolean =
        moduleName == MODULE && entityName in stubs

    /** Builds stubs for exactly the [names] a module asks for. */
    fun createImports(store: Store, names: Collection<String>): List<Import> = imports(store) {
        for (name in names) {
            val stub = stubs.getValue(name)
            function {
                moduleName = MODULE
                entityName = name
                type {
                    params { stub.params.forEach { if (it == I64) i64() else i32() } }
                    results { if (stub.errno != null) i32() }
                }
                // Arguments are ignored; the only effect is the errno in result slot 0.
                reference { _, results -> if (stub.errno != null) results.writeI32(0, stub.errno) }
            }
        }
    }
}
