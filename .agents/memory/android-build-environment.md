---
name: Android build environment
description: Android Gradle verification requires both Java and an Android SDK path in this workspace.
---

Android Gradle builds in this workspace need a configured Android SDK in addition to the Java toolchain; Java alone does not let Gradle compile the app.

**Why:** The imported project includes an Android module, but the Replit environment initially had neither Java nor an Android SDK path available, so Gradle stopped during configuration before Kotlin compilation.

**How to apply:** Before relying on an Android compile result, check for `ANDROID_HOME` or an SDK directory and configure the SDK through the workspace environment rather than adding a project-local machine-specific path.