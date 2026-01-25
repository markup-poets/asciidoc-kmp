# TCK Results Publisher - Implementation Tasks

## Overview

This implementation plan breaks down the TCK Results Publisher feature into discrete, incremental coding tasks. The feature uses our own AsciiDoc parser and HTML renderer to publish TCK test results to GitHub Pages, providing transparent visibility into certification progress while validating our implementation through dogfooding.

---

## Phase 1: Core Infrastructure (Foundation)

### 1. Core Data Models and Interfaces
- [x] 1.1 Create data models (ExportMetadata, PublishMetadata, PublishResult, PublicationRecord, WorkflowResult)
  - Validates: Requirements 1.1, 5.1, 6.1
  - Details: Create @Serializable data classes for export and publish operations
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/PublisherModels.kt
- [x] 1.2 Create TckResultsExporter interface
  - Validates: Requirements 1.1
  - Details: Define interface for exporting test results to AsciiDoc format
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/TckResultsExporter.kt
- [x] 1.3 Create GitHubPagesPublisher interface
  - Validates: Requirements 5.1
  - Details: Define interface for publishing HTML to GitHub Pages
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/GitHubPagesPublisher.kt
- [x] 1.4 Create TckResultsPublishWorkflow interface
  - Validates: Requirements 6.1
  - Details: Define interface for orchestrating the complete pipeline
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/TckResultsPublishWorkflow.kt
- [x] 1.5 Create PublishConfig data class
  - Validates: Requirements 6.2
  - Details: Configuration for GitHub Pages publishing (repository URL, token, etc.)
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/PublisherModels.kt
- [x] 1.6 Create PublishError sealed class hierarchy
  - Validates: Requirements 6.4
  - Details: Error types for export, parse, render, publish, network, validation errors
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/PublishError.kt

### 2. Results Exporter Implementation
- [x] 2.1 Implement DefaultTckResultsExporter class
  - Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7
  - Details: Generate AsciiDoc document with summary, categories, failed tests, metadata
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/DefaultTckResultsExporter.kt
- [x] 2.2 Write unit tests for DefaultTckResultsExporter
  - Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7
  - Details: Test document structure, summary, categories, failed tests, metadata, edge cases
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/DefaultTckResultsExporterTest.kt
- [ ]* 2.3 Write property test for complete test information export
  - Validates: Property 1, Requirements 1.1, 1.2, 1.3, 1.4
  - Details: Verify all test information is present in exported AsciiDoc
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ExporterPropertiesTest.kt
- [ ]* 2.4 Write property test for organizational structure
  - Validates: Property 2, Requirements 1.5
  - Details: Verify tests are organized by category
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ExporterPropertiesTest.kt
- [ ]* 2.5 Write property test for summary and metadata completeness
  - Validates: Property 3, Requirements 1.6, 1.7
  - Details: Verify summary statistics and metadata are complete
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ExporterPropertiesTest.kt

## Phase 2: Dogfooding Validation (CRITICAL)

### 3. Export-Parse Round-Trip Testing
- [x] 3.1 Create integration test for export-parse round-trip
  - Validates: Requirements 2.1, 8.2, 8.4
  - Details: Export to AsciiDoc, parse with DefaultAsciidocParser, verify success
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ExportParseRoundTripTest.kt
- [ ]* 3.2 Write property test for dogfooding round-trip (CRITICAL)
  - Validates: Property 4, Requirements 2.1, 8.4
  - Details: Verify parser can parse all exported AsciiDoc documents
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/DogfoodingPropertiesTest.kt
- [ ]* 3.3 Write property test for information preservation
  - Validates: Property 5, Requirements 2.3
  - Details: Verify AST contains all essential test result information
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/DogfoodingPropertiesTest.kt
- [x] 3.4 Implement parse error handling
  - Validates: Requirements 2.2, 8.4
  - Details: Add error reporting with line/column info, treat parse failures as critical bugs
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/DefaultTckResultsPublishWorkflow.kt

## Phase 3: HTML Rendering Integration

### 4. Render Configuration and Wrapper
- [x] 4.1 Create render configuration for TCK results
  - Validates: Requirements 3.2, 3.3, 3.4, 3.5
  - Details: Configure RenderConfig with KotlinTheme, output options, custom CSS
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/RenderConfigFactory.kt
- [x] 4.2 Implement rendering wrapper
  - Validates: Requirements 3.1, 3.6
  - Details: Create method to render Document AST to HTML with error handling
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/TckHtmlRenderer.kt
- [x] 4.3 Write integration test for export-parse-render pipeline
  - Validates: Requirements 3.1, 3.2, 3.5
  - Details: Test complete pipeline from results to HTML
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ExportParseRenderTest.kt
- [ ]* 4.4 Write property test for successful HTML rendering
  - Validates: Property 6, Requirements 3.1, 3.2, 3.5
  - Details: Verify all valid ASTs render to HTML successfully
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/RenderingPropertiesTest.kt
- [ ]* 4.5 Write property test for visual status differentiation
  - Validates: Property 7, Requirements 3.4
  - Details: Verify different test statuses have different visual indicators
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/RenderingPropertiesTest.kt
- [ ]* 4.6 Write property test for HTML structure validation
  - Validates: Property 16, Requirements 8.5
  - Details: Verify HTML contains expected structural elements
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/RenderingPropertiesTest.kt

## Phase 4: GitHub Pages Publisher

### 5. Publisher Implementation
- [x] 5.1 Implement DefaultGitHubPagesPublisher class (JVM-only initially)
  - Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.6
  - Details: Use existing PlatformGitOperations for gh-pages branch operations
  - Location: tck-quality-testing/src/jvmMain/kotlin/org/markup/poet/tck/publisher/DefaultGitHubPagesPublisher.kt
- [x] 5.2 Implement index page generator
  - Validates: Requirements 5.4, 7.2
  - Details: Generate HTML index with Kotlin theme, list historical results
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/IndexPageGenerator.kt
- [x] 5.3 Write unit tests for publisher with mocked Git operations
  - Validates: Requirements 5.1, 5.5
  - Details: Test archiving, index generation, error handling
  - Location: tck-quality-testing/src/jvmTest/kotlin/org/markup/poet/tck/publisher/DefaultGitHubPagesPublisherTest.kt
- [ ]* 5.4 Write property test for historical preservation
  - Validates: Property 10, Requirements 5.3, 7.1
  - Details: Verify all previous results are preserved
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/PublisherPropertiesTest.kt
- [ ]* 5.5 Write property test for index completeness
  - Validates: Property 11, Requirements 5.4, 7.2
  - Details: Verify index contains links to all archived results
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/PublisherPropertiesTest.kt
- [ ]* 5.6 Write property test for asset inclusion
  - Validates: Property 18, Requirements 5.2
  - Details: Verify all necessary files are included in publication
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/PublisherPropertiesTest.kt
- [ ]* 5.7 Write property test for public URL generation
  - Validates: Property 17, Requirements 5.6
  - Details: Verify public URL format is correct
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/PublisherPropertiesTest.kt

## Phase 5: Change Detection and Comparison

### 6. Publication History and Change Detection
- [ ] 6.1 Create publication history tracker
  - Validates: Requirements 7.1, 7.3
  - Details: Implement storage for previous publication records
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/PublicationHistoryTracker.kt
- [ ] 6.2 Implement change detection logic
  - Validates: Requirements 7.3, 7.4, 7.5
  - Details: Compare current results with previous, identify status changes
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/ChangeDetector.kt
- [ ] 6.3 Update exporter to include change indicators
  - Validates: Requirements 7.4, 7.5
  - Details: Add "newly passing" and "regression" markers to report
  - Location: Modify DefaultTckResultsExporter.kt
- [ ]* 6.4 Write property test for change detection
  - Validates: Property 12, Requirements 7.3, 7.4, 7.5
  - Details: Verify change detection correctly identifies status changes
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ChangeDetectionPropertiesTest.kt
- [ ]* 6.5 Write unit tests for comparison logic
  - Validates: Requirements 7.3, 7.4, 7.5
  - Details: Test with no previous results, identical results, changes
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ChangeDetectorTest.kt

## Phase 6: Workflow Orchestrator

### 7. Complete Pipeline Implementation
- [x] 7.1 Implement DefaultTckResultsPublishWorkflow class
  - Validates: Requirements 6.1, 6.3, 6.4, 6.5
  - Details: Orchestrate export → parse → render → publish pipeline
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/DefaultTckResultsPublishWorkflow.kt
- [x] 7.2 Add validation steps to workflow
  - Validates: Requirements 8.5
  - Details: Validate AsciiDoc, AST, HTML, publication at each stage
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/WorkflowValidator.kt
- [ ] 7.3 Write integration tests for complete workflow
  - Validates: Requirements 6.1, 6.4
  - Details: Test end-to-end execution, failure at each stage, dry-run mode
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/WorkflowIntegrationTest.kt
- [ ]* 7.4 Write property test for pipeline stage logging
  - Validates: Property 13, Requirements 6.3
  - Details: Verify progress is logged at each stage
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/WorkflowPropertiesTest.kt
- [ ]* 7.5 Write property test for error propagation
  - Validates: Property 14, Requirements 6.4
  - Details: Verify errors stop pipeline and are reported
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/WorkflowPropertiesTest.kt
- [ ]* 7.6 Write property test for execution time reporting
  - Validates: Property 15, Requirements 6.5
  - Details: Verify execution time is tracked and reported
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/WorkflowPropertiesTest.kt

## Phase 7: Conformance Report Enhancements

### 8. Enhanced Reporting Features
- [ ] 8.1 Enhance exporter with detailed conformance information
  - Validates: Requirements 4.1, 4.2, 4.3, 4.5, 4.6
  - Details: Verify pass rates by category, certification status, add recommendations
  - Location: Modify DefaultTckResultsExporter.kt
- [ ] 8.2 Implement recommendations generator
  - Validates: Requirements 4.5
  - Details: Analyze results and generate actionable recommendations
  - Location: tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/RecommendationsGenerator.kt
- [ ]* 8.3 Write property test for failed test details
  - Validates: Property 8, Requirements 4.4
  - Details: Verify all failed tests include error details
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ConformancePropertiesTest.kt
- [ ]* 8.4 Write property test for recommendations generation
  - Validates: Property 9, Requirements 4.5
  - Details: Verify recommendations are generated when certification not ready
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ConformancePropertiesTest.kt
- [ ]* 8.5 Write unit tests for conformance report sections
  - Validates: Requirements 4.1, 4.2, 4.3, 4.5
  - Details: Test pass rate calculations, certification status, recommendations
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/ConformanceReportTest.kt

## Phase 8: Gradle Tasks and CLI Integration

### 9. Gradle Task Implementation
- [x] 9.1 Create publishTckResults Gradle task
  - Validates: Requirements 6.2
  - Details: Add task to tck-quality-testing module with configuration options
  - Location: tck-quality-testing/build.gradle.kts
- [x] 9.2 Create configuration file support
  - Validates: Requirements 6.2
  - Details: Load PublishConfig from file or environment variables
  - Location: tck-quality-testing/src/jvmMain/kotlin/org/markup/poet/tck/publisher/ConfigLoader.kt
- [ ]* 9.3 Write unit tests for Gradle task
  - Validates: Requirements 6.2
  - Details: Test task execution, configuration loading, dry-run mode
  - Location: tck-quality-testing/src/jvmTest/kotlin/org/markup/poet/tck/publisher/GradleTaskTest.kt

## Phase 9: CI/CD Integration

### 10. GitHub Actions Workflow
- [ ] 10.1 Create GitHub Actions workflow file
  - Validates: Requirements 6.1
  - Details: Define triggers, TCK test execution, results publishing
  - Location: .github/workflows/publish-tck-results.yml
- [ ] 10.2 Add PR comment integration
  - Validates: Requirements 6.1
  - Details: Post comment with results URL and pass rate summary on PRs
  - Location: .github/workflows/publish-tck-results.yml
- [ ] 10.3 Create workflow documentation
  - Validates: Requirements 6.1
  - Details: Document manual trigger, configuration, troubleshooting
  - Location: tck-quality-testing/docs/PUBLISHING_WORKFLOW.md

## Phase 10: Security and Validation

### 11. Security Implementation
- [ ] 11.1 Verify input sanitization implementation
  - Validates: Requirements 8.1, 8.2, 8.3
  - Details: Verify sanitization of test names, error messages, filenames
  - Location: Review DefaultTckResultsExporter.sanitizeForAsciidoc()
- [ ] 11.2 Implement GitHub token security
  - Validates: Requirements 5.1
  - Details: Load token from environment only, never log, validate format
  - Location: tck-quality-testing/src/jvmMain/kotlin/org/markup/poet/tck/publisher/TokenManager.kt
- [ ]* 11.3 Write unit tests for sanitization
  - Validates: Requirements 8.1
  - Details: Test with special characters, long strings, malicious patterns
  - Location: tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/publisher/SanitizationTest.kt

## Phase 11: Documentation and Examples

### 12. User Documentation
- [x] 12.1 Write user documentation
  - Validates: Requirements 6.2
  - Details: Document local usage, configuration, CI/CD setup, troubleshooting
  - Location: tck-quality-testing/docs/TCK_RESULTS_PUBLISHER.md
- [ ] 12.2 Create example configurations
  - Validates: Requirements 6.2
  - Details: Examples for local development, CI/CD, different repository setups
  - Location: tck-quality-testing/docs/examples/
- [ ] 12.3 Add inline code documentation
  - Validates: All requirements
  - Details: Verify KDoc comments on all public interfaces, add usage examples
  - Location: Review all publisher package files

## Phase 12: Final Verification

### 13. System Verification
- [ ] 13.1 Run full TCK suite and publish results
  - Validates: All requirements
  - Details: Execute complete workflow with real TCK results
- [ ] 13.2 Verify published page accessibility
  - Validates: Requirements 5.6
  - Details: Check public URL is accessible and displays correctly
- [ ] 13.3 Verify visual styling and responsiveness
  - Validates: Requirements 3.2, 3.3, 3.4
  - Details: Test on desktop and mobile devices
- [ ] 13.4 Verify historical results preservation
  - Validates: Requirements 5.3, 7.1
  - Details: Check all previous results are accessible
- [ ] 13.5 Test manual trigger workflow
  - Validates: Requirements 6.1
  - Details: Verify GitHub Actions manual trigger works correctly

---

## Notes

- Tasks marked with `*` are optional property-based tests (can be skipped for faster MVP)
- Each task references specific requirements for traceability
- Phases ensure incremental validation and testing
- The dogfooding round-trip test (3.1) is CRITICAL - if it fails, it indicates a bug in our parser or exporter
- Initial implementation targets JVM only; other platforms can be added later
- GitHub token must be provided via environment variable for security
- The feature uses existing parser and renderer components (no new parsing/rendering code needed)
- Git operations infrastructure already exists via `PlatformGitOperations` - reuse for publisher
