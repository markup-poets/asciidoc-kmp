package org.markup.poet.tck.fixtures

import java.io.File
import java.net.URI

/**
 * JVM implementation of ResourceLoader using ClassLoader.
 */
internal actual object ResourceLoader {
    
    actual fun readResource(path: String): String? {
        return try {
            // Try to read from classpath resources
            val classLoader = ResourceLoader::class.java.classLoader
            val inputStream = classLoader.getResourceAsStream(path)
            inputStream?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun listResources(path: String): List<String> {
        return try {
            val classLoader = ResourceLoader::class.java.classLoader
            val resource = classLoader.getResource(path)
            
            if (resource != null) {
                val uri = resource.toURI()
                
                // Handle both file system and JAR resources
                if (uri.scheme == "file") {
                    // File system
                    val directory = File(uri)
                    if (directory.exists() && directory.isDirectory) {
                        directory.listFiles()?.map { it.name } ?: emptyList()
                    } else {
                        emptyList()
                    }
                } else {
                    // JAR or other archive - try to list using file system
                    try {
                        val fileSystem = java.nio.file.FileSystems.newFileSystem(uri, emptyMap<String, Any>())
                        val dirPath = fileSystem.getPath(path)
                        java.nio.file.Files.list(dirPath).use { stream ->
                            stream.map { it.fileName.toString() }.toList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
