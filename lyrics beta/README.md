# Lyrics

Standalone Android app for searching Apple Music lyrics, importing TTML/LRC files, exporting lyrics, and customizing lyric display.

This project contains its own lyrics UI, Apple Music login/client, lyric parser, bundled `am-lyrics` WebView assets, player, cache manager, and status-bar lyric notification implementation. Runtime code is copied into this project and does not reference the parent Apple Music app's source tree.

## Build

Open this directory in Android Studio, or run:

```powershell
.\gradlew.bat :lyrics-tool:assembleDebug
```

The debug APK is written to `lyrics-tool/build/outputs/apk/debug/lyrics-tool-debug.apk`.

The Android SDK must be configured in Android Studio or through `ANDROID_HOME`/`ANDROID_SDK_ROOT`.
