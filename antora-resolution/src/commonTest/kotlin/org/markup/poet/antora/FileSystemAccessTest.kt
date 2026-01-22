package org.markup.poet.antora

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class FileSystemAccessTest {
    
    private val fileSystem = DefaultFileSystemAccess()
    
    @Test
    fun `should detect non-existent file`() {
        val exists = fileSystem.exists("/nonexistent/path/to/file.txt")
        assertFalse(exists)
    }
    
    @Test
    fun `should return error for non-existent file read`() {
        val result = fileSystem.readFile("/nonexistent/path/to/file.txt")
        assertTrue(result is FileReadResult.Error)
    }
    
    @Test
    fun `should return empty list for non-existent directory`() {
        val entries = fileSystem.listDirectory("/nonexistent/directory")
        assertTrue(entries.isEmpty())
    }
    
    @Test
    fun `should detect non-directory as not directory`() {
        // This test uses a path that's unlikely to be a directory
        val isDir = fileSystem.isDirectory("/nonexistent/file.txt")
        assertFalse(isDir)
    }
}
