package org.markup.poet.tck.reporting

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class DefaultReportGeneratorTest {
    private val generator = DefaultReportGenerator()
    
    @Test
    fun `should generate valid JUnit XML for passed test`() {
        val summary = TestSummary(
            totalTests = 1,
            passed = 1,
            failed = 0,
            skipped = 0,
            pending = 0,
            duration = 1.seconds,
            results = listOf(
                TestResult(
                    testName = "test1",
                    platform = "jvm",
                    status = TestStatus.PASSED,
                    duration = 1.seconds
                )
            )
        )
        
        val xml = generator.generateJUnitXml(summary)
        
        assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(xml.contains("<testsuite"))
        assertTrue(xml.contains("tests=\"1\""))
        assertTrue(xml.contains("failures=\"0\""))
        assertTrue(xml.contains("<testcase"))
        assertTrue(xml.contains("name=\"test1\""))
    }
    
    @Test
    fun `should generate JUnit XML with failure information`() {
        val summary = TestSummary(
            totalTests = 1,
            passed = 0,
            failed = 1,
            skipped = 0,
            pending = 0,
            duration = 1.seconds,
            results = listOf(
                TestResult(
                    testName = "test1",
                    platform = "jvm",
                    status = TestStatus.FAILED,
                    duration = 1.seconds,
                    errorMessage = "Test failed",
                    stackTrace = "at line 1"
                )
            )
        )
        
        val xml = generator.generateJUnitXml(summary)
        
        assertTrue(xml.contains("<failure"))
        assertTrue(xml.contains("Test failed"))
        assertTrue(xml.contains("at line 1"))
    }
    
    @Test
    fun `should escape XML special characters`() {
        val summary = TestSummary(
            totalTests = 1,
            passed = 0,
            failed = 1,
            skipped = 0,
            pending = 0,
            duration = 1.seconds,
            results = listOf(
                TestResult(
                    testName = "test<>&\"'",
                    platform = "jvm",
                    status = TestStatus.FAILED,
                    duration = 1.seconds,
                    errorMessage = "Error with <>&\"'"
                )
            )
        )
        
        val xml = generator.generateJUnitXml(summary)
        
        assertTrue(xml.contains("&lt;"))
        assertTrue(xml.contains("&gt;"))
        assertTrue(xml.contains("&amp;"))
        assertTrue(xml.contains("&quot;"))
        assertTrue(xml.contains("&apos;"))
    }
    
    @Test
    fun `should generate JSON report`() {
        val summary = TestSummary(
            totalTests = 2,
            passed = 1,
            failed = 1,
            skipped = 0,
            pending = 0,
            duration = 2.seconds,
            results = listOf(
                TestResult("test1", "jvm", TestStatus.PASSED, 1.seconds),
                TestResult("test2", "jvm", TestStatus.FAILED, 1.seconds, "Error")
            )
        )
        
        val json = generator.generateJson(summary)
        
        assertTrue(json.contains("\"totalTests\": 2"))
        assertTrue(json.contains("\"passed\": 1"))
        assertTrue(json.contains("\"failed\": 1"))
        assertTrue(json.contains("\"testName\": \"test1\""))
        assertTrue(json.contains("\"testName\": \"test2\""))
    }
    
    @Test
    fun `should escape JSON special characters`() {
        val summary = TestSummary(
            totalTests = 1,
            passed = 0,
            failed = 1,
            skipped = 0,
            pending = 0,
            duration = 1.seconds,
            results = listOf(
                TestResult(
                    testName = "test",
                    platform = "jvm",
                    status = TestStatus.FAILED,
                    duration = 1.seconds,
                    errorMessage = "Error with \"quotes\" and \n newlines"
                )
            )
        )
        
        val json = generator.generateJson(summary)
        
        assertTrue(json.contains("\\\""))
        assertTrue(json.contains("\\n"))
    }
    
    @Test
    fun `should generate text report`() {
        val summary = TestSummary(
            totalTests = 3,
            passed = 1,
            failed = 1,
            skipped = 0,
            pending = 1,
            duration = 3.seconds,
            results = listOf(
                TestResult("test1", "jvm", TestStatus.PASSED, 1.seconds),
                TestResult("test2", "jvm", TestStatus.FAILED, 1.seconds, "Error"),
                TestResult("test3", "jvm", TestStatus.PENDING, 1.seconds, "Not implemented")
            )
        )
        
        val text = generator.generateText(summary)
        
        assertTrue(text.contains("Total Tests: 3"))
        assertTrue(text.contains("Passed:      1"))
        assertTrue(text.contains("Failed:      1"))
        assertTrue(text.contains("Pending:     1"))
        assertTrue(text.contains("Failed Tests:"))
        assertTrue(text.contains("Pending Tests:"))
    }
}
