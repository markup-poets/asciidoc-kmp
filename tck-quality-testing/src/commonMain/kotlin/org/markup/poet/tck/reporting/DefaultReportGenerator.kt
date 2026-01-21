package org.markup.poet.tck.reporting

/**
 * Default implementation of ReportGenerator.
 */
class DefaultReportGenerator : ReportGenerator {
    override fun generateJUnitXml(summary: TestSummary): String {
        val xml = StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xml.append("<testsuite ")
        xml.append("name=\"TCK Test Suite\" ")
        xml.append("tests=\"${summary.totalTests}\" ")
        xml.append("failures=\"${summary.failed}\" ")
        xml.append("skipped=\"${summary.skipped}\" ")
        xml.append("time=\"${summary.duration.inWholeMilliseconds / 1000.0}\"")
        xml.append(">\n")
        
        for (result in summary.results) {
            xml.append("  <testcase ")
            xml.append("name=\"${escapeXml(result.testName)}\" ")
            xml.append("classname=\"${escapeXml(result.platform)}\" ")
            xml.append("time=\"${result.duration.inWholeMilliseconds / 1000.0}\"")
            
            when (result.status) {
                TestStatus.PASSED -> {
                    xml.append(" />\n")
                }
                TestStatus.FAILED -> {
                    xml.append(">\n")
                    xml.append("    <failure message=\"${escapeXml(result.errorMessage ?: "Test failed")}\">\n")
                    if (result.stackTrace != null) {
                        xml.append("${escapeXml(result.stackTrace)}\n")
                    }
                    xml.append("    </failure>\n")
                    xml.append("  </testcase>\n")
                }
                TestStatus.SKIPPED -> {
                    xml.append(">\n")
                    xml.append("    <skipped />\n")
                    xml.append("  </testcase>\n")
                }
                TestStatus.PENDING -> {
                    xml.append(">\n")
                    xml.append("    <skipped message=\"${escapeXml(result.errorMessage ?: "Pending")}\" />\n")
                    xml.append("  </testcase>\n")
                }
            }
        }
        
        xml.append("</testsuite>")
        return xml.toString()
    }
    
    override fun generateJson(summary: TestSummary): String {
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"totalTests\": ${summary.totalTests},\n")
        json.append("  \"passed\": ${summary.passed},\n")
        json.append("  \"failed\": ${summary.failed},\n")
        json.append("  \"skipped\": ${summary.skipped},\n")
        json.append("  \"pending\": ${summary.pending},\n")
        json.append("  \"duration\": \"${summary.duration}\",\n")
        json.append("  \"results\": [\n")
        
        summary.results.forEachIndexed { index, result ->
            json.append("    {\n")
            json.append("      \"testName\": \"${escapeJson(result.testName)}\",\n")
            json.append("      \"platform\": \"${escapeJson(result.platform)}\",\n")
            json.append("      \"status\": \"${result.status}\",\n")
            json.append("      \"duration\": \"${result.duration}\"")
            
            if (result.errorMessage != null) {
                json.append(",\n      \"errorMessage\": \"${escapeJson(result.errorMessage)}\"")
            }
            if (result.stackTrace != null) {
                json.append(",\n      \"stackTrace\": \"${escapeJson(result.stackTrace)}\"")
            }
            
            json.append("\n    }")
            if (index < summary.results.size - 1) {
                json.append(",")
            }
            json.append("\n")
        }
        
        json.append("  ]\n")
        json.append("}")
        return json.toString()
    }
    
    override fun generateText(summary: TestSummary): String {
        val text = StringBuilder()
        text.append("TCK Test Suite Results\n")
        text.append("======================\n\n")
        text.append("Total Tests: ${summary.totalTests}\n")
        text.append("Passed:      ${summary.passed}\n")
        text.append("Failed:      ${summary.failed}\n")
        text.append("Skipped:     ${summary.skipped}\n")
        text.append("Pending:     ${summary.pending}\n")
        text.append("Duration:    ${summary.duration}\n\n")
        
        if (summary.failed > 0) {
            text.append("Failed Tests:\n")
            text.append("-------------\n")
            summary.results.filter { it.status == TestStatus.FAILED }.forEach { result ->
                text.append("- ${result.testName} (${result.platform})\n")
                if (result.errorMessage != null) {
                    text.append("  Error: ${result.errorMessage}\n")
                }
            }
            text.append("\n")
        }
        
        if (summary.pending > 0) {
            text.append("Pending Tests:\n")
            text.append("--------------\n")
            summary.results.filter { it.status == TestStatus.PENDING }.forEach { result ->
                text.append("- ${result.testName} (${result.platform})\n")
                if (result.errorMessage != null) {
                    text.append("  Reason: ${result.errorMessage}\n")
                }
            }
        }
        
        return text.toString()
    }
    
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
    
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
