package org.markup.poet.asciidoc.export

import java.io.File
import java.io.IOException

/**
 * Android implementation of file writing functionality.
 * Uses the same JVM file APIs available on Android.
 */
internal actual class FileWriter {
    actual fun writeToFile(path: String, content: String): FileWriteResult {
        return try {
            val file = File(path)
            
            // Create parent directories if they don't exist
            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }
            
            // Write content to file
            file.writeText(content)
            
            FileWriteResult.Success(path)
        } catch (e: IOException) {
            FileWriteResult.Error(path, "Failed to write file: ${e.message}", e)
        } catch (e: SecurityException) {
            FileWriteResult.Error(path, "Permission denied: ${e.message}", e)
        } catch (e: Exception) {
            FileWriteResult.Error(path, "Unexpected error: ${e.message}", e)
        }
    }
}
