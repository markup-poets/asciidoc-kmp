package org.markup.poet.tck.execution

import kotlinx.serialization.json.*

/**
 * Compares two JSON strings semantically, ignoring formatting differences.
 * 
 * This comparator:
 * - Parses both JSON strings
 * - Compares structure and values
 * - Ignores whitespace and key order
 * - Provides detailed diff information on mismatch
 */
object JsonComparator {
    
    /**
     * Compare two JSON strings semantically.
     * 
     * @param expected Expected JSON string
     * @param actual Actual JSON string
     * @return ValidationResult indicating success or failure with diff
     */
    fun compare(expected: String, actual: String): ValidationResult {
        return try {
            val expectedJson = Json.parseToJsonElement(expected)
            val actualJson = Json.parseToJsonElement(actual)
            
            compareJsonElements(expectedJson, actualJson, "root")
        } catch (e: Exception) {
            ValidationResult.Failure("JSON parsing error: ${e.message}")
        }
    }
    
    /**
     * Compare two JsonElement objects recursively.
     */
    private fun compareJsonElements(
        expected: JsonElement,
        actual: JsonElement,
        path: String
    ): ValidationResult {
        // Check type match
        if (expected::class != actual::class) {
            return ValidationResult.Failure(
                "Type mismatch at $path: expected ${expected::class.simpleName}, got ${actual::class.simpleName}"
            )
        }
        
        return when (expected) {
            is JsonObject -> compareJsonObjects(expected, actual as JsonObject, path)
            is JsonArray -> compareJsonArrays(expected, actual as JsonArray, path)
            is JsonPrimitive -> compareJsonPrimitives(expected, actual as JsonPrimitive, path)
            else -> ValidationResult.Failure("Unknown JSON type at $path")
        }
    }
    
    /**
     * Compare two JSON objects.
     */
    private fun compareJsonObjects(
        expected: JsonObject,
        actual: JsonObject,
        path: String
    ): ValidationResult {
        // Check all expected keys are present
        for (key in expected.keys) {
            if (!actual.containsKey(key)) {
                return ValidationResult.Failure("Missing key at $path: '$key'")
            }
            
            val result = compareJsonElements(
                expected[key]!!,
                actual[key]!!,
                "$path.$key"
            )
            if (result is ValidationResult.Failure) {
                return result
            }
        }
        
        // Check for unexpected keys
        for (key in actual.keys) {
            if (!expected.containsKey(key)) {
                return ValidationResult.Failure("Unexpected key at $path: '$key'")
            }
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Compare two JSON arrays.
     */
    private fun compareJsonArrays(
        expected: JsonArray,
        actual: JsonArray,
        path: String
    ): ValidationResult {
        if (expected.size != actual.size) {
            return ValidationResult.Failure(
                "Array size mismatch at $path: expected ${expected.size}, got ${actual.size}"
            )
        }
        
        for (i in expected.indices) {
            val result = compareJsonElements(
                expected[i],
                actual[i],
                "$path[$i]"
            )
            if (result is ValidationResult.Failure) {
                return result
            }
        }
        
        return ValidationResult.Success
    }
    
    /**
     * Compare two JSON primitives.
     */
    private fun compareJsonPrimitives(
        expected: JsonPrimitive,
        actual: JsonPrimitive,
        path: String
    ): ValidationResult {
        // Handle null
        if (expected.isString != actual.isString) {
            return ValidationResult.Failure(
                "Type mismatch at $path: expected ${if (expected.isString) "string" else "number/boolean"}, " +
                "got ${if (actual.isString) "string" else "number/boolean"}"
            )
        }
        
        // Compare content
        if (expected.content != actual.content) {
            return ValidationResult.Failure(
                "Value mismatch at $path: expected '${expected.content}', got '${actual.content}'"
            )
        }
        
        return ValidationResult.Success
    }
}
