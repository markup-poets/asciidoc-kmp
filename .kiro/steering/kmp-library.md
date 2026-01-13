---
inclusion: always
---
# Technology Stack

## Build System
- **Gradle** with Kotlin DSL (`.gradle.kts` files)
- **Version Catalog** (`gradle/libs.versions.toml`) for dependency management

## Core Technologies
- **Kotlin Multiplatform** 2.2.20
- **Android Gradle Plugin** 8.13.0
- **Vanniktech Maven Publish Plugin** 0.34.0 for Maven Central publishing

## Platform Configuration
- **Android**: minSdk 24, compileSdk 36, JVM target 11
- **JVM**: Target JVM 11
- **iOS**: x64, ARM64, Simulator ARM64
- **Linux**: x64

## Dependencies
- `kotlin-test` for unit testing across all platforms

## Common Commands

### Build
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test                    # All tests
./gradlew :library:testDebugUnitTest  # Android tests
./gradlew :library:jvmTest        # JVM tests
./gradlew :library:iosX64Test     # iOS tests
```

### Publishing
```bash
./gradlew publishToMavenLocal     # Local Maven repository
./gradlew publishToMavenCentral   # Maven Central (requires setup)
```

### Clean
```bash
./gradlew clean
```

## Architecture Patterns
- **expect/actual declarations** for platform-specific implementations
- **Common source sets** for shared code
- **Platform-specific source sets** for platform implementations
- **Package structure**: All library components use `org.markup.poet` as the base package name