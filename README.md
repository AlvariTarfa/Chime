# Chime
Chime: a goal-first Android launcher that cuts screen time with mindful pauses, focus modes, and daily goal tracking. Built with Kotlin and Jetpack Compose.

## Build environment

Open this repository in GitHub Codespaces with **Code → Codespaces → Create codespace**. The dev container installs JDK 17, the Android command-line SDK, and Gradle through SDKMAN. It does not include an emulator.

After the Android project and Gradle wrapper are in place, build the debug APK from the repository root:

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`. Download it from the Codespace to your computer, connect your Android phone with USB debugging enabled, then sideload it with Android Platform Tools:

```bash
adb install -r app-debug.apk
```
