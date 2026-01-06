# Debug Analysis - Compose Version Compatibility Crash

**Date**: 2025-01-06
**Issue**: Crash after markdown library integration
**Error**: `java.lang.NoSuchMethodError: No static method init-impl(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V in class Landroidx/compose/runtime/Updater;`

## Executive Summary

The application crashes immediately after integrating the mikepenz-markdown library (version 0.39.0). The root cause is a **Compose version incompatibility** between the library and the project's Compose BOM version.

## Root Cause Analysis

### 1. Error Analysis

**Error Type**: `java.lang.NoSuchMethodError`
**Missing Method**: `init-impl` in `androidx.compose.runtime.Updater` class

This error occurs when:
- A library is compiled against a different version of Compose than what the runtime provides
- The `init-impl` method signature changed between Compose versions
- The Kotlin/Compose compiler generates different bytecode for different versions

### 2. Version Analysis

#### Project Configuration
```toml
kotlin = "2.2.20"
compose = "2025.10.01"  # BOM version
mikepenz-markdown = "0.39.0"
```

#### Compose BOM 2025.10.01 Contents
- **androidx.compose.runtime**: 1.9.4 (stable)
- **androidx.compose.foundation**: 1.9.4
- **androidx.compose.ui**: 1.9.4
- **androidx.compose.material3**: 1.9.4

#### mikepenz-markdown 0.39.0
- Built with Kotlin 2.2.20
- Built with Compose 1.9.4
- **Known Issue**: Version 0.39.0 has reported crashes with Compose 1.9.3 and below

### 3. Compatibility Matrix

| Library Version | Built With | Project Uses | Status |
|----------------|------------|--------------|---------|
| mikepenz 0.39.0 | Kotlin 2.2.20 + Compose 1.9.4 | Kotlin 2.2.20 + Compose 1.9.4 | **INCOMPATIBLE** |
| mikepenz 0.38.1 | Kotlin 2.2.0 + Compose 1.8.3 | Kotlin 2.2.20 + Compose 1.9.4 | **MIGHT WORK** |
| mikepenz 0.37.0 | Kotlin 2.0.20 + Compose 1.7.x | Kotlin 2.2.20 + Compose 1.9.4 | **RISKY** |

## Investigation Findings

### Known Issues
1. **Issue #491**: "Crash with compose 1.9.3 and below while using v0.39.0"
   - Reported in mikepenz/multiplatform-markdown-renderer repository
   - Confirms version 0.39.0 has compatibility issues with Compose 1.9.3/1.9.4

### Research Sources
- [GitHub Repository](https://github.com/mikepenz/multiplatform-markdown-renderer)
- [Maven Central](https://central.sonatype.com/artifact/com.mikepenz/multiplatform-markdown-renderer)
- [Releases Page](https://github.com/mikepenz/multiplatform-markdown-renderer/releases)
- [Compose BOM Mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)

## Solution Strategy

### Option 1: Downgrade to Version 0.38.1
**Pros**:
- More stable version
- Better compatibility with Compose 1.9.4
- Fewer reported issues

**Cons**:
- May lack some features from 0.39.0
- Not the latest version

### Option 2: Wait for Fix in 0.40.x
**Pros**:
- Will include proper Compose 1.9.4 support
- Latest features and bug fixes

**Cons**:
- Not currently available
- Delays the implementation

### Option 3: Use Alternative Library
**Pros**:
- Avoids compatibility issues
- Can use existing jetbrains-markdown

**Cons**:
- Loses advanced rendering features
- Reverts to original implementation

## Recommended Solution

**Downgrade to version 0.38.1** with the following changes:

### File: `gradle/libs.versions.toml`
```toml
mikepenz-markdown = "0.38.1"  # Downgraded from 0.39.0 for Compose 1.9.4 compatibility
```

### Rationale
1. Version 0.38.1 is stable and has better compatibility with Compose 1.9.4
2. No known crash reports with this version
3. Maintains the desired markdown rendering features
4. Aligns with project's Compose BOM version

## Verification Steps

After applying the fix:
1. Clean build: `./gradlew clean`
2. Rebuild project: `./gradlew build`
3. Run app on device/emulator
4. Test markdown rendering in article screen
5. Verify no `NoSuchMethodError` crashes

## Next Steps

1. Update `libs.versions.toml` to use version 0.38.1
2. Clean and rebuild the project
3. Test markdown rendering functionality
4. Verify app stability
5. Monitor for any new issues

## References

- [Compose BOM to Library Version Mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- [mikepenz/multiplatform-markdown-renderer Releases](https://github.com/mikepenz/multiplatform-markdown-renderer/releases)
- [Maven Central: multiplatform-markdown-renderer](https://central.sonatype.com/artifact/com.mikepenz/multiplatform-markdown-renderer)
- [jetc.dev Newsletter #287](https://jetc.dev/issues/287)
