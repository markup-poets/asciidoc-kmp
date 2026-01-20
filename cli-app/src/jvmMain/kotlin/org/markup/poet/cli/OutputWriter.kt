package org.markup.poet.cli

import java.io.File

/**
 * Interface for writing output content to different destinations.
 */
interface OutputWriter {
    /**
     * Write content to the output destination.
     * 
     * @param content The content to write
     * @throws Exception if writing fails
     */
    fun write(content: String)
}

/**
 * Writes output to a file, creating parent directories if needed.
 */
class FileOutputWriter(private val file: File) : OutputWriter {
    override fun write(content: String) {
        // Create parent directories if they don't exist
        file.parentFile?.mkdirs()
        
        // Write content to file
        file.writeText(content)
    }
}

/**
 * Writes output to standard output (stdout).
 */
class StdoutOutputWriter : OutputWriter {
    override fun write(content: String) {
        print(content)
    }
}
