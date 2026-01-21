package org.markup.poet.tck.fixtures

import java.io.File

/**
 * Android implementation of ResourceLoader using ClassLoader.
 * Similar to JVM implementation since Android runs on JVM.
 */
internal actual object ResourceLoader {
    
    actual fun readResource(path: String): String? {
        return try {
            // Try to read from classpath resources
            val classLoader = ResourceLoader::class.java.classLoader
            val inputStream = classLoader?.getResourceAsStream(path)
            inputStream?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun listResources(path: String): List<String> {
        return try {
            val classLoader = ResourceLoader::class.java.classLoader
            val resource = classLoader?.getResource(path)
            
            if (resource != null) {
                val uri = resource.toURI()
                
                // Handle file system resources
                if (uri.scheme == "file") {
                    val directory = File(uri)
                    if (directory.exists() && directory.isDirectory) {
                        directory.listFiles()?.map { it.name } ?: emptyList()
                    } else {
                        emptyList()
                    }
                } else {
                    // For Android assets, we may need a different approach
                    // For now, return empty list
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
