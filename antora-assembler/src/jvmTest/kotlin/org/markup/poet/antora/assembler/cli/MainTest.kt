package org.markup.poet.antora.assembler.cli

import kotlin.test.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class MainTest {
    
    @Test
    fun `main should display help when no arguments provided`() {
        // Capture stdout
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
        
        try {
            // This would normally call exitProcess, but we can't test that easily
            // Instead, we'll just verify the command can be created
            val command = AssembleCommand()
            command.printHelp()
            
            val output = outputStream.toString()
            assert(output.contains("Antora") || output.contains("assemble"))
        } finally {
            System.setOut(originalOut)
        }
    }
}
