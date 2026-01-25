# TCK Results Publisher - User Documentation

## Overview

The TCK Results Publisher is a dogfooding feature that uses AsciiDoc Konvert's own parsing and rendering pipeline to publish official TCK test results to GitHub Pages. This provides transparent visibility into certification progress while validating that our implementation works correctly on real-world content.

### Key Features

- **Dogfooding**: Uses our own AsciiDoc parser and HTML renderer exclusively
- **Transparent**: Publishes all results publicly, including failures
- **Automated**: Integrates with CI/CD for automatic updates
- **Historical Tracking**: Preserves previous results to show progress over time
- **Visual Appeal**: Uses Kotlin theme for professional, on-brand presentation

### Pipeline Architecture

The system follows a four-stage pipeline:

1. **Export**: Convert TCK test results to AsciiDoc format
2. **Parse**: Parse the AsciiDoc using our own parser (dogfooding!)
3. **Render**: Render the AST to HTML with Kotlin theme styling
4. **Publish**: Commit the HTML to GitHub Pages with historical archiving

## Local Usage

### Prerequisites

- JDK 11 or higher
- Gradle 8.0 or higher
- Git configured with GitHub credentials
- GitHub repository with Pages enabled

### Running Locally

#### Generate HTML Without Publishing (Dry Run)

```bash
# Generate HTML and save locally for preview
./gradlew :tck-quality-testing:generateTckResultsHtml

# View the generated HTML
open tck-quality-testing/build/tck-results/latest.html
```

#### Publish to GitHub Pages

```bash
# Set your GitHub token (required for publishing)
export GITHUB_TOKEN=your_personal_access_token

# Run TCK tests and publish results
./gradlew :tck-quality-testing:publishTckResults
```

#### Run with Custom Configuration

```bash
# Use a configuration file
./gradlew :tck-quality-testing:publishTckResults \
  -PpublishConfig=path/to/config.json

# Override specific settings
./gradlew :tck-quality-testing:publishTckResults \
  -PgithubToken=$GITHUB_TOKEN \
  -PrepositoryUrl=github.com/user/repo.git \
  -Pbranch=gh-pages
```

### Configuration File Format

Create a `publish-config.json` file:

```json
{
  "repositoryUrl": "github.com/user/repo.git",
  "branch": "gh-pages",
  "baseUrl": "https://user.github.io/repo",
  "authorName": "TCK Bot",
  "authorEmail": "tck-bot@example.com",
  "commitMessage": "Update TCK results"
}
```

**Note**: Never include `githubToken` in the configuration file. Always use environment variables for security.

## CI/CD Integration

### GitHub Actions Setup

Create `.github/workflows/publish-tck-results.yml`:

```yaml
name: Publish TCK Results

on:
  push:
    branches: [main]
  schedule:
    - cron: '0 0 * * *'  # Daily at midnight UTC
  workflow_dispatch:      # Manual trigger

jobs:
  publish-tck-results:
    runs-on: ubuntu-latest
    
    permissions:
      contents: write      # Required to push to gh-pages
      pages: write         # Required to trigger Pages build
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Run TCK Tests
        run: ./gradlew :tck-quality-testing:jvmTest --tests "OfficialTckTest"
      
      - name: Publish Results
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: ./gradlew :tck-quality-testing:publishTckResults
      
      - name: Comment on PR
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const resultsUrl = fs.readFileSync('build/tck-results-url.txt', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `📊 TCK Results published: [View Results](${resultsUrl})`
            })
```

### Required Secrets

The workflow uses `secrets.GITHUB_TOKEN` which is automatically provided by GitHub Actions. No additional secrets are required.

### Manual Trigger

You can manually trigger the workflow from the GitHub Actions UI:

1. Go to your repository on GitHub
2. Click "Actions" tab
3. Select "Publish TCK Results" workflow
4. Click "Run workflow" button
5. Select the branch and click "Run workflow"

## Configuration

### GitHub Token Setup

#### For Local Development

Create a Personal Access Token (PAT) with the following permissions:

1. Go to GitHub Settings → Developer settings → Personal access tokens → Fine-grained tokens
2. Click "Generate new token"
3. Set token name: "TCK Results Publisher"
4. Set expiration: 90 days (or as needed)
5. Select repository access: Only select repositories → Choose your repo
6. Set permissions:
   - Contents: Read and write
   - Pages: Read and write
7. Click "Generate token"
8. Copy the token and set it as an environment variable:

```bash
export GITHUB_TOKEN=github_pat_xxxxxxxxxxxxx
```

#### For CI/CD

GitHub Actions automatically provides `GITHUB_TOKEN` with appropriate permissions. No additional setup is required.

### Repository Configuration

#### Enable GitHub Pages

1. Go to repository Settings → Pages
2. Source: Deploy from a branch
3. Branch: `gh-pages` / `(root)`
4. Click "Save"

#### Configure gh-pages Branch

The publisher will automatically create and manage the `gh-pages` branch. No manual setup is required.

### Publish Configuration Options

| Option | Description | Default | Required |
|--------|-------------|---------|----------|
| `githubToken` | GitHub personal access token | (from env) | Yes |
| `repositoryUrl` | Repository URL | (auto-detected) | Yes |
| `branch` | Target branch for publishing | `gh-pages` | No |
| `baseUrl` | Base URL for GitHub Pages | (auto-generated) | No |
| `authorName` | Git commit author name | `TCK Bot` | No |
| `authorEmail` | Git commit author email | `tck-bot@example.com` | No |
| `commitMessage` | Git commit message | `Update TCK results` | No |

## Output Format

### Published Structure

The publisher creates the following structure on the `gh-pages` branch:

```
gh-pages/
├── index.html              # Index page with links to all results
├── latest.html             # Most recent results
├── results/
│   ├── 2026-01-24-103000.html
│   ├── 2026-01-23-153000.html
│   └── 2026-01-22-093000.html
└── assets/
    └── (any additional assets if needed)
```

### Index Page

The index page provides:

- Link to latest results
- Summary of latest run (pass rate, total tests)
- Historical results table with:
  - Timestamp
  - Pass rate
  - Test counts
  - Spec version
  - Link to detailed results
- Progress chart showing pass rate over time

### Results Page

Each results page includes:

- **Summary Section**: Overall pass rate, certification status, test counts
- **Test Results by Category**: Organized by test category (block, inline, attribute, etc.)
- **Failed Tests Section**: Detailed error messages and expected vs actual output
- **Metadata Section**: Timestamp, spec version, TCK commit hash, library version, platforms tested

### Visual Styling

All pages use the Kotlin theme with:

- Dark background (#1E1E1E)
- Red accent color (#E44857)
- Professional typography
- Responsive design for mobile devices
- Color-coded test statuses:
  - ✅ Green for passed tests
  - ❌ Red for failed tests
  - ⚠️ Orange for errors
  - ⏸️ Gray for pending/skipped tests

## Troubleshooting

### Common Issues

#### "Parse failed on our own output"

**Symptom**: The workflow fails with a parse error after exporting to AsciiDoc.

**Cause**: This is a CRITICAL bug indicating either:
- The exporter generated invalid AsciiDoc
- The parser has a bug and cannot parse valid AsciiDoc

**Solution**:
1. Check the generated AsciiDoc in `build/tck-results/export.adoc`
2. Try parsing it manually with the parser
3. Report the issue with the generated AsciiDoc attached
4. This should be treated as a high-priority bug

#### "GitHub token authentication failed"

**Symptom**: Publishing fails with authentication error.

**Cause**: Invalid or expired GitHub token, or insufficient permissions.

**Solution**:
1. Verify the token is set correctly: `echo $GITHUB_TOKEN`
2. Check token permissions (Contents: write, Pages: write)
3. Regenerate the token if expired
4. For CI/CD, verify the workflow has correct permissions

#### "Git push failed: rejected"

**Symptom**: Publishing fails when pushing to gh-pages branch.

**Cause**: Branch protection rules or merge conflicts.

**Solution**:
1. Check branch protection rules for gh-pages
2. Ensure the token has write access
3. Try deleting and recreating the gh-pages branch
4. Check for merge conflicts in the branch

#### "Render failed: theme not found"

**Symptom**: Rendering fails with theme error.

**Cause**: Kotlin theme not available or misconfigured.

**Solution**:
1. Verify the html-renderer module is included
2. Check that KotlinTheme is properly registered
3. Rebuild the project: `./gradlew clean build`

### Debug Mode

Enable debug logging to see detailed information:

```bash
./gradlew :tck-quality-testing:publishTckResults --debug
```

This will show:
- Each pipeline stage execution
- Generated AsciiDoc content
- Parse result details
- Render configuration
- Git operations

### Dry Run Mode

Test the workflow without actually publishing:

```bash
./gradlew :tck-quality-testing:publishTckResults --dry-run
```

This will:
- Run all stages except publishing
- Save generated files locally
- Show what would be published
- Validate the complete pipeline

### Manual Recovery

If publishing fails but HTML was generated successfully:

1. Find the generated HTML: `build/tck-results/latest.html`
2. Manually commit to gh-pages:

```bash
# Clone the gh-pages branch
git clone -b gh-pages https://github.com/user/repo.git gh-pages-temp
cd gh-pages-temp

# Copy the generated HTML
cp ../build/tck-results/latest.html .
cp ../build/tck-results/latest.html results/$(date +%Y-%m-%d-%H%M%S).html

# Commit and push
git add .
git commit -m "Manual update of TCK results"
git push origin gh-pages

# Clean up
cd ..
rm -rf gh-pages-temp
```

## Examples

### Example 1: Local Development Workflow

```bash
# 1. Run TCK tests
./gradlew :tck-quality-testing:jvmTest --tests "OfficialTckTest"

# 2. Generate HTML locally (no publishing)
./gradlew :tck-quality-testing:generateTckResultsHtml

# 3. Preview the results
open tck-quality-testing/build/tck-results/latest.html

# 4. If satisfied, publish to GitHub Pages
export GITHUB_TOKEN=your_token
./gradlew :tck-quality-testing:publishTckResults
```

### Example 2: CI/CD with PR Comments

```yaml
# .github/workflows/pr-tck-check.yml
name: PR TCK Check

on:
  pull_request:
    branches: [main]

jobs:
  tck-check:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Run TCK Tests
        run: ./gradlew :tck-quality-testing:jvmTest --tests "OfficialTckTest"
      
      - name: Generate Results (No Publish)
        run: ./gradlew :tck-quality-testing:generateTckResultsHtml
      
      - name: Extract Pass Rate
        id: pass_rate
        run: |
          PASS_RATE=$(grep -oP 'Pass Rate: \K[0-9.]+' build/tck-results/summary.txt)
          echo "pass_rate=$PASS_RATE" >> $GITHUB_OUTPUT
      
      - name: Comment on PR
        uses: actions/github-script@v6
        with:
          script: |
            const passRate = '${{ steps.pass_rate.outputs.pass_rate }}';
            const emoji = passRate >= 90 ? '🎉' : passRate >= 75 ? '✅' : '⚠️';
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `${emoji} TCK Pass Rate: **${passRate}%**`
            })
```

### Example 3: Custom Configuration

```bash
# Create custom configuration
cat > my-publish-config.json << EOF
{
  "repositoryUrl": "github.com/myorg/myrepo.git",
  "branch": "docs",
  "baseUrl": "https://myorg.github.io/myrepo",
  "authorName": "CI Bot",
  "authorEmail": "ci@myorg.com",
  "commitMessage": "Update TCK certification results [skip ci]"
}
EOF

# Publish with custom configuration
export GITHUB_TOKEN=your_token
./gradlew :tck-quality-testing:publishTckResults \
  -PpublishConfig=my-publish-config.json
```

## Best Practices

### Security

1. **Never commit tokens**: Always use environment variables or secrets
2. **Rotate tokens regularly**: Set expiration dates on personal access tokens
3. **Use fine-grained tokens**: Limit permissions to only what's needed
4. **Review token usage**: Regularly audit which tokens have access

### Performance

1. **Run tests before publishing**: Don't publish if tests haven't run
2. **Use caching**: Cache Gradle dependencies in CI/CD
3. **Limit history**: Consider archiving very old results to reduce repository size
4. **Optimize images**: If adding images, compress them first

### Maintenance

1. **Monitor pass rates**: Set up alerts for significant drops in pass rate
2. **Review failures regularly**: Don't let failing tests accumulate
3. **Update documentation**: Keep this guide updated with new features
4. **Clean up old results**: Periodically archive results older than 1 year

### Workflow Integration

1. **Publish on main branch only**: Avoid publishing from feature branches
2. **Use PR checks**: Run tests on PRs but don't publish
3. **Schedule regular runs**: Use cron to publish daily or weekly
4. **Manual triggers**: Keep manual trigger option for ad-hoc runs

## API Reference

### Gradle Tasks

#### `publishTckResults`

Runs TCK tests and publishes results to GitHub Pages.

**Usage**:
```bash
./gradlew :tck-quality-testing:publishTckResults [options]
```

**Options**:
- `-PgithubToken=<token>`: GitHub personal access token
- `-PrepositoryUrl=<url>`: Repository URL
- `-Pbranch=<branch>`: Target branch (default: gh-pages)
- `-PpublishConfig=<path>`: Path to configuration file
- `--dry-run`: Generate HTML without publishing

**Output**:
- Exit code 0 on success
- Exit code 1 on failure
- Prints public URL on success

#### `generateTckResultsHtml`

Generates HTML from TCK results without publishing.

**Usage**:
```bash
./gradlew :tck-quality-testing:generateTckResultsHtml
```

**Output**:
- HTML file: `build/tck-results/latest.html`
- Summary file: `build/tck-results/summary.txt`
- AsciiDoc file: `build/tck-results/export.adoc`

### Programmatic API

#### TckResultsPublishWorkflow

```kotlin
interface TckResultsPublishWorkflow {
    suspend fun execute(results: AggregatedResults): Result<WorkflowResult>
}

// Usage
val workflow = DefaultTckResultsPublishWorkflow(
    exporter = DefaultTckResultsExporter(),
    parser = DefaultAsciidocParser(),
    renderer = DefaultHtmlRenderer(),
    publisher = DefaultGitHubPagesPublisher(),
    config = PublishConfig(
        githubToken = System.getenv("GITHUB_TOKEN"),
        repositoryUrl = "github.com/user/repo.git"
    )
)

val result = workflow.execute(tckResults)
if (result.isSuccess) {
    println("Published to: ${result.getOrThrow().publicUrl}")
} else {
    println("Failed: ${result.exceptionOrNull()?.message}")
}
```

## Support

### Getting Help

- **Documentation**: This guide and the design document
- **Issues**: Report bugs on GitHub Issues
- **Discussions**: Ask questions on GitHub Discussions
- **Code**: Review the implementation in `tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/publisher/`

### Reporting Issues

When reporting issues, please include:

1. **Error message**: Full error output
2. **Generated AsciiDoc**: If parse failed, attach the generated AsciiDoc
3. **Configuration**: Your publish configuration (without tokens!)
4. **Environment**: OS, JDK version, Gradle version
5. **Steps to reproduce**: Exact commands you ran

### Contributing

Contributions are welcome! Please:

1. Read the design document first
2. Write tests for new features
3. Follow the existing code style
4. Update documentation
5. Submit a pull request

## Changelog

### Version 1.0.0 (2026-01-25)

- Initial release
- Export TCK results to AsciiDoc
- Parse with our own parser (dogfooding)
- Render to HTML with Kotlin theme
- Publish to GitHub Pages
- Historical tracking
- CI/CD integration
- Comprehensive documentation

## License

This feature is part of AsciiDoc Konvert and is licensed under the same license as the main project.
