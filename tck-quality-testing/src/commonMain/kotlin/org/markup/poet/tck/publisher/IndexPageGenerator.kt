package org.markup.poet.tck.publisher

import kotlin.math.roundToInt

/**
 * Generator for the index page that lists all published TCK results.
 *
 * The index page provides:
 * - Link to the latest results
 * - Summary of the latest run (pass rate, total tests)
 * - Historical results table with timestamp, pass rate, test counts, spec version, and links
 * - Progress chart showing pass rate over time
 *
 * The page uses the Kotlin theme for consistent styling with the results pages.
 *
 * ## Example Usage
 * ```kotlin
 * val generator = IndexPageGenerator()
 * val publications = listOf(
 *     PublicationRecord(
 *         runId = "2026-01-24-103000",
 *         timestamp = 1737715800000,
 *         publicUrl = "https://user.github.io/repo/results/2026-01-24-103000.html",
 *         passRate = 0.769,
 *         totalTests = 13,
 *         passedTests = 10,
 *         specVersion = "1.0.0",
 *         tckCommitHash = "abc123",
 *         libraryVersion = "1.0.0",
 *         platforms = listOf("JVM", "iOS")
 *     )
 * )
 * val html = generator.generate(publications).getOrThrow()
 * ```
 */
class IndexPageGenerator {
    
    /**
     * Generate an HTML index page for all published results.
     *
     * @param publications List of all published results, sorted by timestamp (newest first)
     * @return Result containing the HTML string, or an error
     */
    fun generate(publications: List<PublicationRecord>): Result<String> {
        return try {
            if (publications.isEmpty()) {
                Result.success(generateEmptyIndex())
            } else {
                Result.success(generateIndex(publications))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate index page: ${e.message}", e))
        }
    }
    
    private fun generateEmptyIndex(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>AsciiDoc Konvert - TCK Results History</title>
                ${generateStyles()}
            </head>
            <body>
                <div class="container">
                    <h1>AsciiDoc Konvert - TCK Certification Results</h1>
                    <p class="subtitle">No results published yet.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun generateIndex(publications: List<PublicationRecord>): String {
        val latest = publications.first()
        
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>AsciiDoc Konvert - TCK Results History</title>
                ${generateStyles()}
            </head>
            <body>
                <div class="container">
                    <h1>AsciiDoc Konvert - TCK Certification Results</h1>
                    <p class="subtitle">Transparent visibility into our certification progress</p>
                    
                    ${generateLatestSection(latest)}
                    ${generateHistoricalSection(publications)}
                    ${generateProgressChart(publications)}
                    
                    <footer>
                        <p>Generated using AsciiDoc Konvert's own parser and renderer (dogfooding!)</p>
                        <p>Last updated: ${formatTimestamp(latest.timestamp)}</p>
                    </footer>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun generateLatestSection(latest: PublicationRecord): String {
        val passRatePercent = (latest.passRate * 100).roundToInt()
        val passRateClass = getPassRateClass(latest.passRate)
        
        return """
            <section class="latest-results">
                <h2>Latest Results</h2>
                <div class="latest-card">
                    <div class="latest-link">
                        <a href="latest.html" class="btn-primary">View Latest Results</a>
                    </div>
                    <div class="latest-stats">
                        <div class="stat">
                            <span class="stat-label">Pass Rate</span>
                            <span class="stat-value $passRateClass">$passRatePercent%</span>
                        </div>
                        <div class="stat">
                            <span class="stat-label">Tests</span>
                            <span class="stat-value">${latest.passedTests}/${latest.totalTests}</span>
                        </div>
                        <div class="stat">
                            <span class="stat-label">Spec Version</span>
                            <span class="stat-value">${latest.specVersion}</span>
                        </div>
                        <div class="stat">
                            <span class="stat-label">Generated</span>
                            <span class="stat-value">${formatTimestamp(latest.timestamp)}</span>
                        </div>
                    </div>
                </div>
            </section>
        """.trimIndent()
    }
    
    private fun generateHistoricalSection(publications: List<PublicationRecord>): String {
        val rows = publications.joinToString("\n") { pub ->
            val passRatePercent = (pub.passRate * 100).roundToInt()
            val passRateClass = getPassRateClass(pub.passRate)
            val relativeUrl = pub.publicUrl.substringAfter("/").substringAfter("/").substringAfter("/")
            
            """
                <tr>
                    <td>${formatTimestamp(pub.timestamp)}</td>
                    <td class="$passRateClass">$passRatePercent%</td>
                    <td>${pub.passedTests}/${pub.totalTests}</td>
                    <td>${pub.specVersion}</td>
                    <td>${pub.platforms.joinToString(", ")}</td>
                    <td><a href="$relativeUrl" class="btn-link">View</a></td>
                </tr>
            """.trimIndent()
        }
        
        return """
            <section class="historical-results">
                <h2>Historical Results</h2>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Pass Rate</th>
                                <th>Tests</th>
                                <th>Spec Version</th>
                                <th>Platforms</th>
                                <th>Link</th>
                            </tr>
                        </thead>
                        <tbody>
                            $rows
                        </tbody>
                    </table>
                </div>
            </section>
        """.trimIndent()
    }
    
    private fun generateProgressChart(publications: List<PublicationRecord>): String {
        if (publications.size < 2) {
            return "" // Not enough data for a chart
        }
        
        // Take up to the last 10 publications for the chart
        val chartData = publications.take(10).reversed()
        val maxPassRate = 100
        val chartHeight = 20
        
        // Generate ASCII chart
        val chart = buildString {
            for (i in chartHeight downTo 0) {
                val threshold = (i.toDouble() / chartHeight) * maxPassRate
                append(String.format("%3d%% |", threshold.toInt()))
                
                for (pub in chartData) {
                    val passRatePercent = (pub.passRate * 100).roundToInt()
                    if (passRatePercent >= threshold) {
                        append(" ● ")
                    } else {
                        append("   ")
                    }
                }
                append("\n")
            }
            
            append("     |")
            append("_".repeat(chartData.size * 3))
            append("\n      ")
            
            for (pub in chartData) {
                val date = formatShortDate(pub.timestamp)
                append(date.take(3))
            }
        }
        
        return """
            <section class="progress-chart">
                <h2>Progress Over Time</h2>
                <pre class="chart">$chart</pre>
            </section>
        """.trimIndent()
    }
    
    private fun generateStyles(): String {
        return """
            <style>
                /* Kotlin Theme Colors */
                :root {
                    --bg-primary: #1E1E1E;
                    --bg-secondary: #2B2B2B;
                    --bg-tertiary: #3C3C3C;
                    --text-primary: #FFFFFF;
                    --text-secondary: #CCCCCC;
                    --text-muted: #999999;
                    --accent-primary: #E44857;
                    --accent-hover: #FF5566;
                    --success: #10B981;
                    --warning: #F59E0B;
                    --error: #EF4444;
                    --border: #4A4A4A;
                }
                
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                    background-color: var(--bg-primary);
                    color: var(--text-primary);
                    line-height: 1.6;
                    padding: 2rem;
                }
                
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                }
                
                h1 {
                    font-size: 2.5rem;
                    margin-bottom: 0.5rem;
                    color: var(--accent-primary);
                }
                
                h2 {
                    font-size: 1.8rem;
                    margin-top: 2rem;
                    margin-bottom: 1rem;
                    color: var(--text-primary);
                }
                
                .subtitle {
                    font-size: 1.1rem;
                    color: var(--text-secondary);
                    margin-bottom: 2rem;
                }
                
                /* Latest Results Section */
                .latest-results {
                    margin-bottom: 3rem;
                }
                
                .latest-card {
                    background-color: var(--bg-secondary);
                    border: 1px solid var(--border);
                    border-radius: 8px;
                    padding: 2rem;
                }
                
                .latest-link {
                    text-align: center;
                    margin-bottom: 2rem;
                }
                
                .btn-primary {
                    display: inline-block;
                    background-color: var(--accent-primary);
                    color: var(--text-primary);
                    padding: 1rem 2rem;
                    border-radius: 4px;
                    text-decoration: none;
                    font-weight: bold;
                    font-size: 1.1rem;
                    transition: background-color 0.2s;
                }
                
                .btn-primary:hover {
                    background-color: var(--accent-hover);
                }
                
                .latest-stats {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 1.5rem;
                }
                
                .stat {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    text-align: center;
                }
                
                .stat-label {
                    font-size: 0.9rem;
                    color: var(--text-muted);
                    margin-bottom: 0.5rem;
                }
                
                .stat-value {
                    font-size: 1.5rem;
                    font-weight: bold;
                    color: var(--text-primary);
                }
                
                /* Historical Results Section */
                .table-container {
                    overflow-x: auto;
                    background-color: var(--bg-secondary);
                    border: 1px solid var(--border);
                    border-radius: 8px;
                }
                
                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                
                thead {
                    background-color: var(--bg-tertiary);
                }
                
                th, td {
                    padding: 1rem;
                    text-align: left;
                    border-bottom: 1px solid var(--border);
                }
                
                th {
                    font-weight: bold;
                    color: var(--text-primary);
                }
                
                td {
                    color: var(--text-secondary);
                }
                
                tbody tr:hover {
                    background-color: var(--bg-tertiary);
                }
                
                tbody tr:last-child td {
                    border-bottom: none;
                }
                
                .btn-link {
                    color: var(--accent-primary);
                    text-decoration: none;
                    font-weight: bold;
                }
                
                .btn-link:hover {
                    color: var(--accent-hover);
                    text-decoration: underline;
                }
                
                /* Pass Rate Colors */
                .pass-rate-high {
                    color: var(--success);
                    font-weight: bold;
                }
                
                .pass-rate-medium {
                    color: var(--warning);
                    font-weight: bold;
                }
                
                .pass-rate-low {
                    color: var(--error);
                    font-weight: bold;
                }
                
                /* Progress Chart */
                .progress-chart {
                    margin-top: 3rem;
                }
                
                .chart {
                    background-color: var(--bg-secondary);
                    border: 1px solid var(--border);
                    border-radius: 8px;
                    padding: 1.5rem;
                    overflow-x: auto;
                    font-family: 'Courier New', monospace;
                    font-size: 0.9rem;
                    line-height: 1.4;
                    color: var(--text-secondary);
                }
                
                /* Footer */
                footer {
                    margin-top: 4rem;
                    padding-top: 2rem;
                    border-top: 1px solid var(--border);
                    text-align: center;
                    color: var(--text-muted);
                    font-size: 0.9rem;
                }
                
                footer p {
                    margin: 0.5rem 0;
                }
                
                /* Responsive Design */
                @media (max-width: 768px) {
                    body {
                        padding: 1rem;
                    }
                    
                    h1 {
                        font-size: 2rem;
                    }
                    
                    h2 {
                        font-size: 1.5rem;
                    }
                    
                    .latest-stats {
                        grid-template-columns: 1fr;
                    }
                    
                    table {
                        font-size: 0.9rem;
                    }
                    
                    th, td {
                        padding: 0.75rem;
                    }
                }
            </style>
        """.trimIndent()
    }
    
    private fun getPassRateClass(passRate: Double): String {
        return when {
            passRate >= 0.90 -> "pass-rate-high"
            passRate >= 0.70 -> "pass-rate-medium"
            else -> "pass-rate-low"
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        // Simple formatting - in production, use a proper date formatter
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'")
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
    
    private fun formatShortDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("MMM dd")
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
}
