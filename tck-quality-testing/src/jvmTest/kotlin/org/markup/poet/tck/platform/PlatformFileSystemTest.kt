package org.markup.poet.tck.platform

import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.processing.DefaultIncludeResolver
import org.markup.poet.asciidoc.processing.FileReadResult
import org.markup.poet.asciidoc.processing.IncludeConfig
import org.markup.poet.asciidoc.processing.IncludeResult
import org.markup.poet.asciidoc.processing.JvmFileReader
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * JVM file-system tests: real file I/O and include path resolution.
 *
 * These are the file-system-dependent counterparts of [PlatformSpecificTest]
 * (encoding/line endings live there in commonTest); they exercise
 * [JvmFileReader] and [DefaultIncludeResolver] against a real temp directory.
 *
 * Requirements: 7.1, 7.3
 */
class PlatformFileSystemTest {

    private val parser = DefaultAsciidocParser()
    private val tempDir: File = File.createTempFile("tck-platform", "").apply {
        delete()
        mkdirs()
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    private fun writeFile(relativePath: String, content: String): File {
        val file = File(tempDir, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
        return file
    }

    private fun resolve(source: String, basePath: String = tempDir.absolutePath): IncludeResult =
        DefaultIncludeResolver(parser).resolve(
            parser.parse(source).document,
            IncludeConfig(basePath = basePath, fileReader = JvmFileReader()),
        )

    private fun IncludeResult.paragraphTexts(): List<String> =
        document.blocks.filterIsInstance<LeafBlock>().map { plainText(it.inlines) }

    // File I/O Tests

    @Test
    fun `should read simple file content`() {
        val file = writeFile("simple.adoc", "This is a simple paragraph.")
        val result = assertIs<FileReadResult.Success>(JvmFileReader().readFile(file.absolutePath))
        assertEquals("This is a simple paragraph.", result.content)
    }

    @Test
    fun `should read multiline file content`() {
        val content = "= Document Title\n\nFirst paragraph.\n\nSecond paragraph with *bold* text."
        val file = writeFile("multiline.adoc", content)
        val result = assertIs<FileReadResult.Success>(JvmFileReader().readFile(file.absolutePath))
        assertEquals(content, result.content)
        assertTrue(result.content.contains("\n"))

        // The read content parses into the expected document structure.
        val document = parser.parse(result.content).document
        assertEquals(2, document.blocks.filterIsInstance<LeafBlock>().size)
    }

    @Test
    fun `should report error for missing file`() {
        val result = JvmFileReader().readFile(File(tempDir, "does-not-exist.adoc").absolutePath)
        val error = assertIs<FileReadResult.Error>(result)
        assertTrue(error.message.isNotBlank())
    }

    // Path Resolution Tests

    @Test
    fun `should resolve absolute paths`() {
        val included = writeFile("chapter.adoc", "Included chapter content.")
        val result = resolve("Before.\n\ninclude::${included.absolutePath}[]", basePath = "")
        assertTrue(result.errors.isEmpty(), "Unexpected errors: ${result.errors}")
        assertEquals(listOf("Before.", "Included chapter content."), result.paragraphTexts())
        assertTrue(result.includedFiles.single().endsWith("chapter.adoc"))
    }

    @Test
    fun `should resolve relative paths with parent directory`() {
        writeFile("shared/common.adoc", "Shared content from parent directory.")
        writeFile("docs/main.adoc", "unused") // establishes the docs/ directory
        val result = resolve(
            "include::../shared/common.adoc[]",
            basePath = File(tempDir, "docs").absolutePath,
        )
        assertTrue(result.errors.isEmpty(), "Unexpected errors: ${result.errors}")
        assertEquals(listOf("Shared content from parent directory."), result.paragraphTexts())
    }

    @Test
    fun `should resolve current directory paths`() {
        writeFile("local.adoc", "Content from the current directory.")
        val result = resolve("include::./local.adoc[]")
        assertTrue(result.errors.isEmpty(), "Unexpected errors: ${result.errors}")
        assertEquals(listOf("Content from the current directory."), result.paragraphTexts())
    }
}
