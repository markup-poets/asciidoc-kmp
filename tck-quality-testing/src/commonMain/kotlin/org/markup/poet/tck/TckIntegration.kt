package org.markup.poet.tck

import org.markup.poet.tck.config.ConfigLoader
import org.markup.poet.tck.config.JsonConfigLoader
import org.markup.poet.tck.config.TckConfig
import org.markup.poet.tck.conformance.CertificationChecker
import org.markup.poet.tck.conformance.ConformanceReport
import org.markup.poet.tck.conformance.DefaultCertificationChecker
import org.markup.poet.tck.conformance.DefaultReportGenerator
import org.markup.poet.tck.conformance.ReportGenerator
import org.markup.poet.tck.conformance.ReportMetadata
import org.markup.poet.tck.execution.AggregatedResults
import org.markup.poet.tck.execution.DefaultResultAggregator
import org.markup.poet.tck.execution.DefaultTestRunner
import org.markup.poet.tck.execution.InMemoryResultCollector
import org.markup.poet.tck.execution.JsonComparator
import org.markup.poet.tck.execution.OutputValidator
import org.markup.poet.tck.execution.ResultAggregator
import org.markup.poet.tck.execution.TestExecutionResult
import org.markup.poet.tck.execution.TestFilter
import org.markup.poet.tck.execution.TestRunner
import org.markup.poet.tck.execution.TestStatus
import org.markup.poet.tck.fixtures.CompositeFixtureLoader
import org.markup.poet.tck.fixtures.DefaultFormatDetector
import org.markup.poet.tck.fixtures.FixtureLoader
import org.markup.poet.tck.fixtures.OfficialTckFixtureLoader
import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import org.markup.poet.tck.fixtures.TestFixture
import org.markup.poet.tck.sync.DefaultTckSyncService
import org.markup.poet.tck.sync.DefaultSyncValidator
import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.TckSyncService
import org.markup.poet.tck.sync.SyncResult
import org.markup.poet.tck.version.DefaultVersionTracker
import org.markup.poet.tck.version.VersionTracker

/**
 * Main entry point for Official AsciiDoc TCK Integration.
 * 
 * This object provides a high-level API for:
 * - Initializing the TCK system
 * - Syncing the official TCK repository
 * - Running tests (custom and/or official)
 * - Generating conformance reports
 * - Checking certification readiness
 * 
 * Example usage:
 * ```kotlin
 * // Initialize with default configuration
 * val context = TckIntegration.initialize()
 * 
 * // Sync official TCK
 * val syncResult = TckIntegration.sync(context)
 * println("Synced ${syncResult.testCount} tests")
 * 
 * // Run all tests
 * val results = TckIntegration.runTests(context)
 * println("Passed: ${results.passedCount}/${results.totalCount}")
 * 
 * // Generate conformance report
 * val report = TckIntegration.generateReport(context, results)
 * println("Report generated: ${report.metadata.generatedAt}")
 * 
 * // Check certification readiness
 * val status = TckIntegration.checkCertification(context, results)
 * println("Certification ready: ${status.isReady}")
 * ```
 */
object TckIntegration {
    
    /**
     * Initialize the TCK system with default or custom configuration.
     * 
     * @param configPath Path to configuration file (default: tck-config.json)
     * @return TckContext with all components initialized
     */
    fun initialize(configPath: String = "tck-quality-testing/tck-config.json"): TckContext {
        val configFileOps = PlatformConfigFileOperations()
        val configLoader = JsonConfigLoader(configFileOps)
        val config = configLoader.loadConfig(configPath)
        
        return createContext(config, configLoader)
    }
    
    /**
     * Sync the official TCK repository.
     * 
     * This will:
     * - Clone the repository if it doesn't exist
     * - Pull latest changes if it exists
     * - Validate repository structure
     * - Update version tracking
     * - Store sync metadata
     * 
     * @param context TCK context
     * @return SyncResult with sync status and metadata
     */
    suspend fun sync(context: TckContext): SyncResult {
        return context.syncService.sync()
    }
    
    /**
     * Run tests based on configuration.
     * 
     * This will:
     * - Load fixtures (custom and/or official based on config)
     * - Apply filters (categories, spec sections)
     * - Execute tests
     * - Collect results
     * - Aggregate results by platform, category, source
     * 
     * @param context TCK context
     * @param filter Optional test filter (default: use config settings)
     * @return AggregatedResults with test execution results
     */
    fun runTests(
        context: TckContext,
        filter: TestFilter? = null
    ): AggregatedResults {
        // Load fixtures based on configuration
        val allFixtures = context.fixtureLoader.loadAllFixtures()
        
        // Apply filter
        val fixtures = if (filter != null) {
            allFixtures.filter { filter.shouldRun(it) }
        } else {
            allFixtures
        }
        
        // Run tests
        val results = context.testRunner.runTests(fixtures)
        
        // Aggregate results
        return context.resultAggregator.aggregate(results)
    }
    
    /**
     * Generate conformance report from test results.
     * 
     * This will:
     * - Generate report with all sections
     * - Include platform, category, and spec breakdowns
     * - List failed and pending tests
     * - Calculate pass rates
     * - Include certification status
     * 
     * @param context TCK context
     * @param results Aggregated test results
     * @return ConformanceReport with all report data
     */
    fun generateReport(
        context: TckContext,
        results: AggregatedResults
    ): ConformanceReport {
        val currentVersion = context.versionTracker.getCurrentVersion()
        val metadata = ReportMetadata(
            generatedAt = currentTimeMillis(),
            specVersion = currentVersion?.specVersion ?: "unknown",
            tckCommitHash = currentVersion?.commitHash ?: "unknown",
            libraryVersion = "1.0.0", // TODO: Get from config or build info
            platforms = listOf(getPlatformName())
        )
        return context.reportGenerator.generateReport(results, metadata)
    }
    
    /**
     * Check certification readiness.
     * 
     * This will:
     * - Check if all requirements are met
     * - Identify blocking issues
     * - Calculate progress towards certification
     * - Generate recommendations
     * 
     * @param context TCK context
     * @param results Aggregated test results
     * @return CertificationStatus with readiness assessment
     */
    fun checkCertification(
        context: TckContext,
        results: AggregatedResults
    ) = context.certificationChecker.checkStatus(results)
    
    /**
     * Complete workflow: sync, run tests, generate report.
     * 
     * This is a convenience method that runs the complete TCK workflow:
     * 1. Sync official TCK (if enabled)
     * 2. Run all tests
     * 3. Generate conformance report
     * 4. Check certification readiness
     * 
     * @param context TCK context
     * @return ConformanceReport with complete results
     */
    suspend fun runCompleteWorkflow(context: TckContext): ConformanceReport {
        // Sync if auto-sync is enabled
        if (context.config.sync.autoSync) {
            sync(context)
        }
        
        // Run tests
        val results = runTests(context)
        
        // Generate report
        return generateReport(context, results)
    }
    
    /**
     * Create a TckContext with the given configuration.
     * 
     * This is an internal method used by initialize() and can be used
     * for testing or custom context creation.
     * 
     * Note: This creates a minimal context. For full functionality,
     * you need to provide parser and renderer implementations.
     */
    internal fun createContext(
        config: TckConfig,
        configLoader: ConfigLoader
    ): TckContext {
        // Create platform-specific operations
        val gitOps: GitOperations = PlatformGitOperations()
        val versionFileOps = PlatformVersionFileOperations()
        
        // Create sync components
        val syncValidator = DefaultSyncValidator()
        val syncService = DefaultTckSyncService(
            repositoryUrl = config.sync.repositoryUrl,
            localPath = config.sync.localPath,
            gitOperations = gitOps,
            validator = syncValidator
        )
        
        // Create version tracking
        val versionTracker = DefaultVersionTracker(
            fileOperations = versionFileOps,
            basePath = config.sync.localPath
        )
        
        // Create fixture loaders
        val customLoader = ResourceFixtureLoader()
        val officialLoader = OfficialTckFixtureLoader(
            tckRepositoryPath = config.sync.localPath
        )
        val formatDetector = DefaultFormatDetector()
        val fixtureLoader = CompositeFixtureLoader(
            loaders = if (config.execution.enableCustomTests && config.execution.enableOfficialTests) {
                listOf(customLoader, officialLoader)
            } else if (config.execution.enableCustomTests) {
                listOf(customLoader)
            } else if (config.execution.enableOfficialTests) {
                listOf(officialLoader)
            } else {
                emptyList()
            },
            formatDetector = formatDetector
        )
        
        // Create test execution components
        // Note: Parser and renderer need to be provided by the application
        val testRunner = createDefaultTestRunner()
        val resultAggregator = DefaultResultAggregator()
        
        // Create reporting components
        val certificationChecker = DefaultCertificationChecker()
        val reportGenerator = DefaultReportGenerator(certificationChecker)
        
        return DefaultTckContext(
            config = config,
            configLoader = configLoader,
            syncService = syncService,
            versionTracker = versionTracker,
            fixtureLoader = fixtureLoader,
            testRunner = testRunner,
            resultAggregator = resultAggregator,
            reportGenerator = reportGenerator,
            certificationChecker = certificationChecker
        )
    }
    
    /**
     * Create a default test runner with your real parser and serializer.
     *
     * This uses:
     * - DefaultAsciidocParser: Your actual AsciiDoc parser
     * - AstJsonSerializer: Converts the ASG to JSON for TCK comparison
     *
     * The serializer automatically detects inline vs block tests based on
     * the test fixture metadata.
     */
    private fun createDefaultTestRunner(): TestRunner {
        val parser = org.markup.poet.asciidoc.parser.DefaultAsciidocParser()
        val serializer = org.markup.poet.tck.serialization.AstJsonSerializer()

        // Create a wrapper that can access fixture metadata
        return object : TestRunner {
            override fun runTest(fixture: TestFixture): TestExecutionResult {
                val startTime = currentTimeMillis()

                return try {
                    // Parse the input
                    val parsed = parser.parse(fixture.input)
                    
                    // Determine serialization mode based on test path
                    val mode = if (fixture.id.contains("/inline/") || 
                                   fixture.metadata["type"] == "inline") {
                        org.markup.poet.tck.serialization.AstJsonSerializer.Mode.INLINE_ONLY
                    } else {
                        org.markup.poet.tck.serialization.AstJsonSerializer.Mode.FULL_DOCUMENT
                    }
                    
                    // Serialize to JSON with appropriate mode
                    val rendered = serializer.serialize(parsed.document, mode)
                    
                    // Validate output if expected output is provided
                    val validationResult = if (fixture.expectedOutput != null) {
                        JsonComparator.compare(fixture.expectedOutput, rendered)
                    } else {
                        org.markup.poet.tck.execution.ValidationResult.Success
                    }
                    
                    val duration = currentTimeMillis() - startTime
                    
                    when (validationResult) {
                        is org.markup.poet.tck.execution.ValidationResult.Success -> TestExecutionResult(
                            fixtureId = fixture.id,
                            status = TestStatus.PASSED,
                            platform = getPlatformName(),
                            durationMs = duration,
                            category = fixture.category,
                            source = fixture.metadata["source"],
                            actualOutput = rendered,
                            expectedOutput = fixture.expectedOutput
                        )
                        is org.markup.poet.tck.execution.ValidationResult.Failure -> TestExecutionResult(
                            fixtureId = fixture.id,
                            status = TestStatus.FAILED,
                            platform = getPlatformName(),
                            durationMs = duration,
                            category = fixture.category,
                            source = fixture.metadata["source"],
                            errorMessage = validationResult.message,
                            actualOutput = rendered,
                            expectedOutput = fixture.expectedOutput,
                            diff = validationResult.diff
                        )
                    }
                } catch (e: org.markup.poet.tck.execution.PendingTestException) {
                    val duration = currentTimeMillis() - startTime
                    TestExecutionResult(
                        fixtureId = fixture.id,
                        status = TestStatus.PENDING,
                        platform = getPlatformName(),
                        durationMs = duration,
                        category = fixture.category,
                        source = fixture.metadata["source"],
                        errorMessage = e.message
                    )
                } catch (e: Exception) {
                    val duration = currentTimeMillis() - startTime
                    TestExecutionResult(
                        fixtureId = fixture.id,
                        status = TestStatus.ERROR,
                        platform = getPlatformName(),
                        durationMs = duration,
                        category = fixture.category,
                        source = fixture.metadata["source"],
                        errorMessage = e.message,
                        stackTrace = e.stackTraceToString()
                    )
                }
            }
            
            override fun runTests(fixtures: List<TestFixture>): List<TestExecutionResult> {
                val results = mutableListOf<TestExecutionResult>()
                val total = fixtures.size
                
                println("\n🚀 Starting TCK test execution...")
                println("   Total tests to run: $total")
                println("   " + "=".repeat(50))
                
                var passed = 0
                var failed = 0
                var errors = 0
                
                fixtures.forEachIndexed { index, fixture ->
                    val testNum = index + 1
                    val progress = (testNum * 100 / total)
                    
                    // Progress logging - show every test
                    println("\n[$testNum/$total] Running: ${fixture.id}")
                    println("   Category: ${fixture.category}")
                    println("   Progress: $progress%")
                    
                    try {
                        val result = runTest(fixture)
                        results.add(result)
                        
                        // Log result
                        when (result.status) {
                            TestStatus.PASSED -> {
                                passed++
                                println("   ✅ PASSED (${result.durationMs}ms)")
                            }
                            TestStatus.FAILED -> {
                                failed++
                                println("   ❌ FAILED (${result.durationMs}ms)")
                                if (result.errorMessage != null) {
                                    val shortError = result.errorMessage.take(100)
                                    println("   Error: $shortError${if (result.errorMessage.length > 100) "..." else ""}")
                                }
                            }
                            TestStatus.ERROR -> {
                                errors++
                                println("   💥 ERROR (${result.durationMs}ms)")
                                if (result.errorMessage != null) {
                                    println("   Error: ${result.errorMessage}")
                                }
                            }
                            TestStatus.PENDING -> {
                                println("   ⏸️  PENDING")
                            }
                            TestStatus.SKIPPED -> {
                                println("   ⏭️  SKIPPED")
                            }
                        }
                        
                        // Log slow tests
                        if (result.durationMs > 1000) {
                            println("   ⚠️  Slow test: took ${result.durationMs}ms")
                        }
                        
                        // Show running summary every 5 tests
                        if (testNum % 5 == 0) {
                            println("\n   📊 Running Summary:")
                            println("      Passed: $passed, Failed: $failed, Errors: $errors")
                            println("      Pass rate: ${if (testNum > 0) passed * 100 / testNum else 0}%")
                        }
                        
                    } catch (e: Exception) {
                        errors++
                        println("   💥 ERROR: ${e.message}")
                        results.add(TestExecutionResult(
                            fixtureId = fixture.id,
                            status = TestStatus.ERROR,
                            platform = getPlatformName(),
                            durationMs = 0,
                            category = fixture.category,
                            source = fixture.metadata["source"],
                            errorMessage = "Test execution failed: ${e.message}",
                            stackTrace = e.stackTraceToString()
                        ))
                    }
                }
                
                println("\n" + "=".repeat(50))
                println("🏁 Test execution complete!")
                println("   Total: $total")
                println("   Passed: $passed (${if (total > 0) passed * 100 / total else 0}%)")
                println("   Failed: $failed")
                println("   Errors: $errors")
                println("=".repeat(50) + "\n")
                
                return results
            }
            
            override fun runTestsFiltered(
                fixtures: List<TestFixture>,
                filter: TestFilter
            ): List<TestExecutionResult> {
                val filtered = fixtures.filter { filter.shouldRun(it) }
                return runTests(filtered)
            }
        }
    }
}

/**
 * Default implementation of TckContext.
 */
internal class DefaultTckContext(
    override val config: TckConfig,
    override val configLoader: ConfigLoader,
    override val syncService: TckSyncService,
    override val versionTracker: VersionTracker,
    override val fixtureLoader: FixtureLoader,
    override val testRunner: TestRunner,
    override val resultAggregator: ResultAggregator,
    override val reportGenerator: ReportGenerator,
    override val certificationChecker: CertificationChecker
) : TckContext {
    
    override fun reloadConfig(configPath: String): TckContext {
        val newConfig = configLoader.loadConfig(configPath)
        return withConfig(newConfig)
    }
    
    override fun withConfig(config: TckConfig): TckContext {
        return TckIntegration.createContext(config, configLoader)
    }
}
