package org.markup.poet.antora

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for AntoraResolver edge cases.
 * Tests specific scenarios including ROOT module handling, invalid coordinates,
 * missing modules, and file not found scenarios.
 */
class AntoraResolverTest {
    
    // Mock FileSystemAccess for testing
    private class MockFileSystemAccess(
        private val existingPaths: Set<String> = emptySet(),
        private val directories: Set<String> = emptySet()
    ) : FileSystemAccess {
        override fun exists(path: String): Boolean {
            val normalized = path.replace('\\', '/')
            return existingPaths.contains(normalized) || directories.contains(normalized)
        }
        
        override fun isDirectory(path: String): Boolean {
            val normalized = path.replace('\\', '/')
            return directories.contains(normalized)
        }
        
        override fun readFile(path: String): FileReadResult {
            val normalized = path.replace('\\', '/')
            return if (existingPaths.contains(normalized)) {
                FileReadResult.Success("mock content")
            } else {
                FileReadResult.Error("File not found: $normalized")
            }
        }
        
        override fun listDirectory(path: String): List<String> {
            return emptyList()
        }
        
        override fun writeFile(path: String, content: String): FileWriteResult {
            return FileWriteResult.Success
        }
    }
    
    @Test
    fun `should resolve partial in ROOT module`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf("/docs/modules/ROOT/partials/intro.adoc"),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/partials")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(componentRoot = "/docs")
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.PARTIAL,
            path = "intro.adoc"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/partials/intro.adoc", result.resolvedPath)
    }
    
    @Test
    fun `should resolve example in named module`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf("/docs/modules/admin/examples/code.java"),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/admin", "/docs/modules/admin/examples")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "admin"
        )
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.EXAMPLE,
            path = "code.java"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/admin/examples/code.java", result.resolvedPath)
    }
    
    @Test
    fun `should resolve page with subdirectories`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf("/docs/modules/ROOT/pages/guides/getting-started.adoc"),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/pages", "/docs/modules/ROOT/pages/guides")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(componentRoot = "/docs")
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.PAGE,
            path = "guides/getting-started.adoc"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/pages/guides/getting-started.adoc", result.resolvedPath)
    }
    
    @Test
    fun `should resolve module-qualified coordinate`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf("/docs/modules/admin/pages/config.adoc"),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/admin", "/docs/modules/admin/pages")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT"
        )
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.PAGE,
            path = "config.adoc",
            module = "admin"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/admin/pages/config.adoc", result.resolvedPath)
    }
    
    @Test
    fun `should resolve component-qualified coordinate`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf("/other-component/modules/ROOT/pages/index.adoc"),
            directories = setOf("/other-component", "/other-component/modules", "/other-component/modules/ROOT", "/other-component/modules/ROOT/pages")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT"
        )
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.PAGE,
            path = "index.adoc",
            module = "ROOT",
            component = "other-component"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/other-component/modules/ROOT/pages/index.adoc", result.resolvedPath)
    }
    
    @Test
    fun `should return MODULE_NOT_FOUND error for missing module`() {
        val fileSystem = MockFileSystemAccess(
            directories = setOf("/docs", "/docs/modules")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(componentRoot = "/docs")
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.PARTIAL,
            path = "intro.adoc",
            module = "nonexistent"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Error)
        assertEquals(ResolutionErrorType.MODULE_NOT_FOUND, result.errorType)
        assertTrue(result.message.contains("nonexistent"))
    }
    
    @Test
    fun `should return FILE_NOT_FOUND error for missing file in existing module`() {
        val fileSystem = MockFileSystemAccess(
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/partials")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(componentRoot = "/docs")
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.PARTIAL,
            path = "missing.adoc"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Error)
        assertEquals(ResolutionErrorType.FILE_NOT_FOUND, result.errorType)
        assertTrue(result.message.contains("missing.adoc"))
    }
    
    @Test
    fun `should resolve relative path from current file`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf(
                "/docs/modules/ROOT/pages/index.adoc",
                "/docs/modules/ROOT/pages/other.adoc"
            ),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/pages")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.RELATIVE,
            path = "other.adoc"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/pages/other.adoc", result.resolvedPath)
    }
    
    @Test
    fun `should resolve relative path with parent directory`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf(
                "/docs/modules/ROOT/pages/guides/tutorial.adoc",
                "/docs/modules/ROOT/pages/index.adoc"
            ),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/pages", "/docs/modules/ROOT/pages/guides")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentFilePath = "/docs/modules/ROOT/pages/guides/tutorial.adoc"
        )
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.RELATIVE,
            path = "../index.adoc"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/pages/index.adoc", result.resolvedPath)
    }
    
    @Test
    fun `should return INVALID_PATH error for relative path without current file context`() {
        val fileSystem = MockFileSystemAccess()
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentFilePath = null
        )
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.RELATIVE,
            path = "other.adoc"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Error)
        assertEquals(ResolutionErrorType.INVALID_PATH, result.errorType)
        assertTrue(result.message.contains("current file context"))
    }
    
    @Test
    fun `should resolve image in ROOT module`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf("/docs/modules/ROOT/images/diagram.png"),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/images")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(componentRoot = "/docs")
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.IMAGE,
            path = "diagram.png"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/images/diagram.png", result.resolvedPath)
    }
    
    @Test
    fun `should resolve attachment in ROOT module`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf("/docs/modules/ROOT/attachments/file.zip"),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/attachments")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(componentRoot = "/docs")
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.ATTACHMENT,
            path = "file.zip"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/attachments/file.zip", result.resolvedPath)
    }
    
    @Test
    fun `resolveInclude should parse coordinate and resolve`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf("/docs/modules/ROOT/partials/intro.adoc"),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/partials")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(componentRoot = "/docs")
        
        val result = resolver.resolveInclude("partial\$intro.adoc", context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/partials/intro.adoc", result.resolvedPath)
    }
    
    @Test
    fun `resolveInclude should treat non-coordinate as relative path`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf(
                "/docs/modules/ROOT/pages/index.adoc",
                "/docs/modules/ROOT/pages/other.adoc"
            ),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/pages")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val result = resolver.resolveInclude("other.adoc", context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/pages/other.adoc", result.resolvedPath)
    }
    
    @Test
    fun `should normalize path with multiple parent directory references`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf(
                "/docs/modules/ROOT/pages/deep/nested/file.adoc",
                "/docs/modules/ROOT/pages/partials/intro.adoc"
            ),
            directories = setOf(
                "/docs", "/docs/modules", "/docs/modules/ROOT", 
                "/docs/modules/ROOT/pages", "/docs/modules/ROOT/pages/deep", "/docs/modules/ROOT/pages/deep/nested",
                "/docs/modules/ROOT/pages/partials"
            )
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentFilePath = "/docs/modules/ROOT/pages/deep/nested/file.adoc"
        )
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.RELATIVE,
            path = "../../partials/intro.adoc"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success, "Expected Success but got: $result")
        assertEquals("/docs/modules/ROOT/pages/partials/intro.adoc", (result as ResolutionResult.Success).resolvedPath)
    }
    
    @Test
    fun `should handle current directory reference in relative path`() {
        val fileSystem = MockFileSystemAccess(
            existingPaths = setOf(
                "/docs/modules/ROOT/pages/index.adoc",
                "/docs/modules/ROOT/pages/other.adoc"
            ),
            directories = setOf("/docs", "/docs/modules", "/docs/modules/ROOT", "/docs/modules/ROOT/pages")
        )
        val resolver = DefaultAntoraResolver(fileSystem)
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val coordinate = ResourceCoordinate(
            type = ResourceType.RELATIVE,
            path = "./other.adoc"
        )
        
        val result = resolver.resolve(coordinate, context)
        
        assertTrue(result is ResolutionResult.Success)
        assertEquals("/docs/modules/ROOT/pages/other.adoc", result.resolvedPath)
    }
}
