package org.markup.poet.tck.sync

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.pclose
import platform.posix.popen
import platform.posix.fgets
import platform.posix.FILE

/**
 * iOS implementation of GitOperations using native git command.
 * 
 * This implementation shells out to the system git command, which must be
 * installed on the system. On iOS, git is typically available through
 * development tools.
 * 
 * **Note**: This implementation does NOT use JavaScript or Node.js.
 * It uses the native system git command.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformGitOperations : GitOperations {
    
    override suspend fun clone(url: String, destination: String, branch: String?): GitResult {
        val branchArg = if (branch != null) "-b $branch" else ""
        val command = "git clone $branchArg $url $destination 2>&1"
        
        return executeCommand(command).let { (exitCode, output) ->
            if (exitCode == 0) {
                GitResult.Success("Successfully cloned repository to $destination")
            } else {
                GitResult.Failure("Git clone failed: $output")
            }
        }
    }
    
    override suspend fun pull(repositoryPath: String): GitResult {
        val command = "cd $repositoryPath && git pull 2>&1"
        
        return executeCommand(command).let { (exitCode, output) ->
            if (exitCode == 0) {
                GitResult.Success("Successfully pulled changes: $output")
            } else {
                GitResult.Failure("Git pull failed: $output")
            }
        }
    }
    
    override fun getCurrentCommitHash(repositoryPath: String): String? {
        val command = "cd $repositoryPath && git rev-parse HEAD 2>&1"
        val (exitCode, output) = executeCommand(command)
        return if (exitCode == 0) output.trim() else null
    }
    
    override fun getCurrentRef(repositoryPath: String): String? {
        val command = "cd $repositoryPath && git rev-parse --abbrev-ref HEAD 2>&1"
        val (exitCode, output) = executeCommand(command)
        return if (exitCode == 0) output.trim() else null
    }
    
    override fun isValidRepository(repositoryPath: String): Boolean {
        val command = "cd $repositoryPath && git rev-parse --git-dir 2>&1"
        val (exitCode, _) = executeCommand(command)
        return exitCode == 0
    }
    
    override fun getRemoteUrl(repositoryPath: String, remoteName: String): String? {
        val command = "cd $repositoryPath && git remote get-url $remoteName 2>&1"
        val (exitCode, output) = executeCommand(command)
        return if (exitCode == 0) output.trim() else null
    }
    
    /**
     * Execute a shell command and return the exit code and output.
     * 
     * @param command The command to execute
     * @return Pair of (exit code, output)
     */
    private fun executeCommand(command: String): Pair<Int, String> {
        val output = StringBuilder()
        val fp: FILE? = popen(command, "r")
        
        if (fp == null) {
            return Pair(1, "Failed to execute command")
        }
        
        try {
            val buffer = ByteArray(4096)
            while (true) {
                val line = fgets(buffer.refTo(0), buffer.size, fp)?.toKString()
                if (line == null) break
                output.append(line)
            }
            
            val exitCode = pclose(fp)
            return Pair(exitCode, output.toString())
        } catch (e: Exception) {
            pclose(fp)
            return Pair(1, "Error executing command: ${e.message}")
        }
    }
}
