# Repository Guidelines

## Project Structure & Module Organization
- `app/` is the Android application module.
- Source code lives under `app/src/main/java/` (Java/Kotlin packages), with the manifest at `app/src/main/AndroidManifest.xml`.
- UI resources are in `app/src/main/res/`, and runtime assets (e.g., media/config) are in `app/src/main/assets/`.
- Gradle build scripts are at the repo root: `build.gradle`, `settings.gradle`, and the wrapper in `gradle/`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew assembleRelease` builds a release APK (uses minify/shrink settings in `app/build.gradle`).
- `./gradlew lintDebug` runs Android Lint for the debug variant.

## Coding Style & Naming Conventions
- Follow standard Android Java/Kotlin style (4-space indentation, braces on same line).
- Classes use `UpperCamelCase`, methods/fields use `lowerCamelCase`, constants use `UPPER_SNAKE_CASE`.
- Keep resource names lowercase with underscores (e.g., `activity_main.xml`).

## Testing Guidelines
- No test suites are currently defined under `app/src/test` or `app/src/androidTest`.
- If you add tests, use standard Android naming: `*Test.kt`/`*Test.java`, and document how to run them.

## Commit & Pull Request Guidelines
- Commit messages in history are short, imperative summaries (e.g., "Add ...", "Refactor ..."). Follow that style.
- PRs should include: a brief description, any relevant log output, and screenshots for UI changes.
- Link related issues when applicable.

## Configuration Notes
- Android SDK path is defined in `local.properties` via `sdk.dir=/home/sun/Android/Sdk`.
- Alternatively, set `ANDROID_SDK_ROOT` to the same path in your shell environment.

## Current Tasks
- Integrate FuckLocation core hooks into `:app` with file-flag config.
- Provide UI for location spoofing toggle and coordinate input.
- Default location spoofing off; ensure toggle state persists.
- Keep location debug logging enabled by default in the UI.
