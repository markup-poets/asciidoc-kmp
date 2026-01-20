# Project Structure

## Root Level
- `build.gradle.kts` - Root build configuration with plugin declarations
- `settings.gradle.kts` - Project settings and module inclusion
- `gradle.properties` - Gradle configuration properties
- `gradle/` - Gradle wrapper and version catalog
  - `libs.versions.toml` - Centralized dependency version management

## Library Module (`library/`)
Main multiplatform library implementation following Kotlin Multiplatform conventions.

### Source Set Organization
```
library/src/
├── commonMain/kotlin/          # Shared code across all platforms
├── commonTest/kotlin/          # Shared tests
├── androidMain/kotlin/         # Android-specific implementations
├── androidHostTest/kotlin/     # Android host tests
├── jvmMain/kotlin/            # JVM-specific implementations  
├── jvmTest/kotlin/            # JVM tests
├── iosMain/kotlin/            # iOS-specific implementations
├── iosTest/kotlin/            # iOS tests
├── linuxX64Main/kotlin/       # Linux-specific implementations
└── linuxX64Test/kotlin/       # Linux tests
```

## Naming Conventions
- **Package**: `org.markup.poet` (consistent across all platforms for all library components)
- **Platform files**: Use descriptive names with platform-specific implementations
- **Test files**: `{Feature}Test.kt` for common tests, `{Platform}{Feature}Test.kt` for platform-specific tests
- **Common files**: Descriptive names based on functionality (e.g., `Parser.kt`, `AstNode.kt`)

## Code Organization Rules
- Use `expect` declarations in `commonMain` for platform-specific APIs
- Implement `actual` declarations in platform-specific source sets
- Keep shared logic in `commonMain/kotlin/`
- Platform-specific implementations should be minimal and focused
- Tests should verify both common functionality and platform-specific behavior

## File Patterns
- Platform implementations: One file per platform with `actual` declarations
- Common code: Shared interfaces and business logic
- Tests: Separate test classes per platform when needed

## .gitkeep

Avoid ceration of .gitkeep in empty folders.