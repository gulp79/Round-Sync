# AGENTS.md

Welcome to the **Round-Sync (rclone-extra fork)** repository. This document is written for AI agents and developer assistants to quickly grasp the project architecture, dependencies, build systems, release workflows, and development guidelines.

---

## 1. Project Overview

- **Repository**: [gulp79/Round-Sync](https://github.com/gulp79/Round-Sync) (Forked from Round-Sync / RCX).
- **Nature**: Android GUI and synchronization utility built on top of [rclone](https://rclone.org/).
- **Core Purpose of this Fork**:
  - Incorporates **[gulp79/rclone-extra](https://github.com/gulp79/rclone-extra)** instead of the upstream official rclone repository.
  - `rclone-extra` provides additional backend providers, fixes, and customizations not available in standard upstream rclone.
  - Maintains an automated, production-ready CI/CD release workflow for building signed multi-architecture Android APKs on-demand and on tag releases.

---

## 2. Architecture & Modules

The repository is structured as a multi-project Gradle setup:

```
Round-Sync/
├── .github/
│   └── workflows/
│       └── build.yml          # Custom CI/CD workflow for signed APK builds & GitHub releases
├── app/                       # Android application module (Kotlin / Java / XML)
│   ├── src/main/java/         # Android UI, services, file sync management, storage providers
│   ├── build.gradle           # APK compilation, split configs, Keystore signing logic
│   └── lib/                   # Target directory where native shared libraries (librclone.so) reside
├── rclone/                    # Native build module for rclone
│   └── build.gradle           # Gradle tasks driving Go cross-compilation with Android NDK
├── safdav/                    # SAF (Storage Access Framework) to WebDAV bridge component
├── gradle/                    # Gradle wrapper files
├── gradle.properties          # Global versions (Go, NDK, rclone version label, JVM args)
├── keystore.properties        # Template/local config for release signing credentials
└── settings.gradle            # Included projects (:app, :rclone, :safdav)
```

---

## 3. The `rclone-extra` Integration Mechanism

Understanding how `rclone` is built and linked into the Android application is critical for any AI agent modifying build or native logic:

1. **Gradle Build Logic (`rclone/build.gradle`)**:
   - The native compilation is driven by custom Gradle tasks (`checkoutRclone`, `buildArm`, `buildArm64`, `buildx86`, `buildx64`, `buildAll`).
   - `checkoutRclone` dynamically checks `https://github.com/gulp79/rclone-extra.git` (`master` branch) via `git ls-remote` to retrieve the latest commit SHA.
   - It configures Go modules with `go mod edit -replace github.com/rclone/rclone=github.com/gulp79/rclone-extra@<commit-sha>`, ensuring dependencies resolve against `rclone-extra`.
   - Uses a dedicated `GOPATH` (`rclone/gopath_env`) and cache directory (`rclone/cache`) to avoid module collision.
   - Cross-compiles for Android ABIs (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) using Clang from the Android NDK specified in `gradle.properties`.
   - Produces `librclone.so` in `app/lib/<abi>/` with `-tags "android noselfupdate"`.

2. **Integration into App**:
   - The Android app consumes `librclone.so` via JNI / CGO bindings to invoke rclone commands and RPC endpoints directly on device.

---

## 4. CI/CD & Signing Workflow (`.github/workflows/build.yml`)

> [!CAUTION]
> **CRITICAL INSTRUCTION FOR AI AGENTS**:
> The `.github/workflows/build.yml` file manages production builds and automated release signing. **Modify this file with extreme caution.**
> Any change must preserve keystore decoding, signing environment variables, and artifact outputs.

### Triggering the Workflow
- **Manual on-demand**: via `workflow_dispatch` in GitHub Actions (supports optional `send_telegram` boolean input).
- **Automated on tag**: triggered on any push matching tag pattern `v*` (e.g., `v2.5.6`).

### Required GitHub Secrets
The workflow expects the following repository secrets to successfully sign APKs:
- `APP_KEYSTORE_BASE64`: Base64-encoded release keystore file (`.keystore` / `.jks`).
- `KEY_STORE_PASSWORD`: Password for the keystore.
- `KEY_ALIAS`: Alias of the signing key.
- `KEY_PASSWORD`: Password for the key alias.
- `TELEGRAM_BOT_TOKEN` & `TELEGRAM_CHAT_ID`: (Optional) Used when Telegram notification is enabled.

### Build & Signing Mechanism
1. The CI runner decodes `APP_KEYSTORE_BASE64` into `app/release.keystore`.
2. Passes signing environment variables (`CMD_RELEASE_KEYSTORE`, `CMD_RELEASE_KEYSTORE_PASSWORD`, `CMD_RELEASE_KEY_ALIAS`, `CMD_RELEASE_KEY_PASSWORD`) to `./gradlew assembleOssRelease`.
3. `app/build.gradle` checks for `CMD_RELEASE_KEYSTORE` and dynamically applies the release signing config.
4. Generates both ABI-split APKs (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) and a `universal` APK under `app/build/outputs/apk/oss/release/`.
5. Uploads artifacts to GitHub Actions artifacts retention and publishes them to GitHub Releases on tag pushes.

---

## 5. Local Development & Building

### Prerequisites
- **JDK**: Java 17+ (JDK 25 used in CI).
- **Go**: 1.20+ (Go 1.26.x specified in CI/properties).
- **Android SDK & NDK**:
  - NDK version: `27.3.13750724` (or as configured in `gradle.properties`).
  - NDK toolchain API: `33`.
  - Android compileSdk: `34`, targetSdk: `35`.

### Local Signing Setup
For local release builds without CI environment variables:
1. Copy or edit `keystore.properties` in the project root:
   ```properties
   storeFile=/path/to/your/release.keystore
   storePassword=yourStorePassword
   keyAlias=yourKeyAlias
   keyPassword=yourKeyPassword
   ```
2. Run standard debug or release Gradle tasks:
   ```bash
   # Build debug APKs (unsigned / debug key)
   ./gradlew assembleOssDebug

   # Build signed release APKs (uses keystore.properties)
   ./gradlew assembleOssRelease
   ```

---

## 6. Guidelines for AI Agents

When assisting or making code changes in this repository, follow these best practices:

1. **Language Policy**:
   - Write all code, code comments, documentation, commit messages, and PR descriptions in **English**.
2. **Protect the Build Pipeline**:
   - Do **NOT** modify `.github/workflows/build.yml` unless specifically requested.
   - When updating `.github/workflows/build.yml`, ensure all environment variables (`CMD_RELEASE_*`), keystore extraction steps, and output artifact paths match `app/build.gradle`.
3. **Respect `rclone-extra` Dependency**:
   - Never replace `https://github.com/gulp79/rclone-extra.git` with upstream `rclone/rclone` unless explicitly requested by the maintainer.
   - If rclone backends or native build flags need changes, check both `rclone/build.gradle` and the upstream `gulp79/rclone-extra` repository compatibility.
4. **Toolchain Versions Alignment**:
   - Keep toolchain versions synchronized across `gradle.properties`, `rclone/build.gradle`, and `.github/workflows/build.yml` (JDK version, Go version, NDK version).
5. **Code Quality & Integrity**:
   - Preserve existing license notices, copyright headers, and formatting conventions.
   - Do not leak keystore paths, credentials, or secrets in code or git commits. Always use environment variables and `.gitignore`-d properties files.
