# Lyrics

Standalone Android app for searching and playing Apple Music songs, importing TTML/LRC files, exporting lyrics, and customizing lyric display.

This project contains its own lyrics UI, Apple Music login/client, lyric parser, bundled `am-lyrics` WebView assets, player, cache manager, and status-bar lyric notification implementation. Runtime code is copied into this project and does not reference the parent Apple Music app's source tree.

Playback and Android media controls share one Media3 player/session so their playback state stays in sync.

Song search and playback remain in Apple Music. Search results request simplified-Chinese metadata and are supplemented from the China storefront when the same recording can be verified by ISRC; the original storefront song ID is retained for playback. Apple Music word-timed lyrics are preferred; otherwise the app tries matching QQ Music QRC lyrics, then the existing online lyric sources. QQ searches use the localized title and artist alongside the original metadata. QQ lyrics use HyperLyrics-Enhanced's `GetPlayLyricInfo` flow: QRC, translation, and romanization are requested together; encrypted lyric text is read from the response's `lyric` field and decrypted as QRC, preserving word timing. The original QRC is parsed first and remains usable when the optional translation is empty or invalid. Separate translation lines are matched one-to-one to original lines by start time within 1 second. No legacy `lyric_download.fcg` request or inferred pairing of interleaved languages is used. When QQ returns a separate translation, it is shown by default and can be toggled; otherwise the original lyric remains visible and translation display stays off. The lyric download menu can also export the decrypted QQ QRC and translation payloads in a `*.qq-qrc.txt` diagnostic file for inspecting source timing/format. QQ candidates are ranked using title, artist, album, and duration; a cross-language top-result fallback still requires a duration difference within 2.5 seconds. Use the lyrics menu (⋮) to select Apple Music, QQ Music, or other online sources manually, or return to the previous lyric source.

The QQ QRC DES helper is adapted from [Lyricify-Lyrics-Helper](https://github.com/WXRIW/Lyricify-Lyrics-Helper) and is licensed under Apache-2.0; its license is included at `lyrics-tool/src/main/java/com/applemusic/lyricstool/data/qq/LICENSE-APACHE-2.0.txt`.

## Build

Open this directory in Android Studio, or run:

```powershell
.\gradlew.bat :lyrics-tool:assembleDebug
```

The debug APK is written to `lyrics-tool/build/outputs/apk/debug/lyrics-tool-debug.apk`.

The Android SDK must be configured in Android Studio or through `ANDROID_HOME`/`ANDROID_SDK_ROOT`.
