package org.markup.poet.tck.config

import kotlinx.cinterop.*
import platform.Foundation.*

/**
 * iOS implementation of ConfigFileOperations using Foundation APIs.
 */
actual class PlatformConfigFileOperations : ConfigFileOperations {
    
    override fun readFile(path: String): String? {
        return try {
            val fileManager = NSFileManager.defaultManager
            
            if (!fileManager.fileExistsAtPath(path)) {
                return null
            }
            
            val data = NSData.dataWithContentsOfFile(path) ?: return null
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
        } catch (e: Exception) {
            null
        }
    }
    
    override fun writeFile(path: String, content: String) {
        val fileManager = NSFileManager.defaultManager
        val nsPath = path as NSString
        
        // Create parent directories if they don't exist
        val parentPath = nsPath.stringByDeletingLastPathComponent
        if (!fileManager.fileExistsAtPath(parentPath)) {
            fileManager.createDirectoryAtPath(
                path = parentPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }
        
        // Write content
        val nsContent = content as NSString
        nsContent.writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
    }
}
