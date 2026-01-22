package org.markup.poet.antora

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ResourceCoordinateTest {
    
    @Test
    fun `should parse simple partial coordinate`() {
        val result = ResourceCoordinate.parse("partial\$file.adoc")
        
        assertNotNull(result)
        assertEquals(ResourceType.PARTIAL, result.type)
        assertEquals("file.adoc", result.path)
        assertNull(result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should parse simple example coordinate`() {
        val result = ResourceCoordinate.parse("example\$code.java")
        
        assertNotNull(result)
        assertEquals(ResourceType.EXAMPLE, result.type)
        assertEquals("code.java", result.path)
        assertNull(result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should parse simple page coordinate`() {
        val result = ResourceCoordinate.parse("page\$other-page.adoc")
        
        assertNotNull(result)
        assertEquals(ResourceType.PAGE, result.type)
        assertEquals("other-page.adoc", result.path)
        assertNull(result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should parse simple image coordinate`() {
        val result = ResourceCoordinate.parse("image\$diagram.png")
        
        assertNotNull(result)
        assertEquals(ResourceType.IMAGE, result.type)
        assertEquals("diagram.png", result.path)
        assertNull(result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should parse simple attachment coordinate`() {
        val result = ResourceCoordinate.parse("attachment\$file.zip")
        
        assertNotNull(result)
        assertEquals(ResourceType.ATTACHMENT, result.type)
        assertEquals("file.zip", result.path)
        assertNull(result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should parse module-qualified coordinate`() {
        val result = ResourceCoordinate.parse("admin:page\$file.adoc")
        
        assertNotNull(result)
        assertEquals(ResourceType.PAGE, result.type)
        assertEquals("file.adoc", result.path)
        assertEquals("admin", result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should parse component-qualified coordinate`() {
        val result = ResourceCoordinate.parse("mycomp:admin:page\$file.adoc")
        
        assertNotNull(result)
        assertEquals(ResourceType.PAGE, result.type)
        assertEquals("file.adoc", result.path)
        assertEquals("admin", result.module)
        assertEquals("mycomp", result.component)
    }
    
    @Test
    fun `should parse relative path without prefix`() {
        val result = ResourceCoordinate.parse("relative/path/file.adoc")
        
        assertNotNull(result)
        assertEquals(ResourceType.RELATIVE, result.type)
        assertEquals("relative/path/file.adoc", result.path)
        assertNull(result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should parse relative path with parent directory`() {
        val result = ResourceCoordinate.parse("../parent.adoc")
        
        assertNotNull(result)
        assertEquals(ResourceType.RELATIVE, result.type)
        assertEquals("../parent.adoc", result.path)
        assertNull(result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should parse path with subdirectories`() {
        val result = ResourceCoordinate.parse("partial\$subdir/file.adoc")
        
        assertNotNull(result)
        assertEquals(ResourceType.PARTIAL, result.type)
        assertEquals("subdir/file.adoc", result.path)
        assertNull(result.module)
        assertNull(result.component)
    }
    
    @Test
    fun `should handle case-insensitive resource types`() {
        val result = ResourceCoordinate.parse("PARTIAL\$file.adoc")
        
        assertNotNull(result)
        assertEquals(ResourceType.PARTIAL, result.type)
        assertEquals("file.adoc", result.path)
    }
    
    @Test
    fun `should return null for blank coordinate`() {
        assertNull(ResourceCoordinate.parse(""))
        assertNull(ResourceCoordinate.parse("   "))
    }
    
    @Test
    fun `should return null for invalid resource type`() {
        assertNull(ResourceCoordinate.parse("invalid\$file.adoc"))
    }
    
    @Test
    fun `should return null for empty path after dollar sign`() {
        assertNull(ResourceCoordinate.parse("partial\$"))
    }
    
    @Test
    fun `should return null for empty module in module-qualified coordinate`() {
        assertNull(ResourceCoordinate.parse(":page\$file.adoc"))
    }
    
    @Test
    fun `should return null for empty component in component-qualified coordinate`() {
        assertNull(ResourceCoordinate.parse(":admin:page\$file.adoc"))
    }
    
    @Test
    fun `should return null for empty module in component-qualified coordinate`() {
        assertNull(ResourceCoordinate.parse("mycomp::page\$file.adoc"))
    }
    
    @Test
    fun `should return null for too many colons`() {
        assertNull(ResourceCoordinate.parse("a:b:c:d:page\$file.adoc"))
    }
}
