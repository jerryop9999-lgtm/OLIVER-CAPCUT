# OLIVER Video Editor
![Build](../../actions/workflows/android-build.yml/badge.svg)

Kotlin + Jetpack Compose video editor. Editing/export uses AndroidX Media3 (ExoPlayer preview, Transformer export).

## Build
- **GitHub Actions:** push to GitHub -> Actions -> *Android Build* -> download artifact `OLIVER-debug-apk`.
- **Release APK:** add secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; a signed release APK is then built too.
- **Local:** JDK 17 + Android SDK 34, then `gradle assembleDebug` (Gradle 8.9) or open in Android Studio.

Requires Android 10+ (minSdk 29). Exports go to `Movies/OLIVER`.
