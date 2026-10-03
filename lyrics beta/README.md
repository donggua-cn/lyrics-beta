# Lyrics

Standalone Android app for searching and playing Apple Music songs, importing TTML/LRC files, exporting lyrics, and customizing lyric display.

This project contains its own lyrics UI, Apple Music login/client, lyric parser, bundled `am-lyrics` WebView assets, player, cache manager, and status-bar lyric notification implementation. Runtime code is copied into this project and does not reference the parent Apple Music app's source tree.

Playback and Android media controls share one Media3 player/session so their playback state stays in sync.

Song search and playback remain in Apple Music. Search first looks up song titles and artists in the Apple Music catalog; if there are no direct matches, it searches QQ Music's lyric index and resolves matching title/artist results back to Apple Music catalog tracks, so lyric searches still play Apple Music versions. Search results request simplified-Chinese metadata and are supplemented from the China storefront when the same recording can be verified by ISRC; the original storefront song ID is retained for playback. When a track is loaded, a floating mini-player appears above the Home/Settings navigation bar with circular album art, title, artist, play/pause, next, and a progress ring around its rounded outer edge; tapping the capsule opens that track's lyrics page. The mini-player uses a mostly opaque surface that lets only a subtle hint of the background show through. The playback controls are also a floating overlay above the full lyrics viewport, with the progress ring around the control card rather than reserving additional lyric-page height. The card background is uniformly translucent and has no drop shadow; playback buttons, status, time, and lyric-timing metadata automatically use a high-contrast accent in dark mode. The live lyrics notification shows the current lyric as its title and the corresponding translation as a second line when available. Swipe up on the playback card to collapse it into a compact floating album-art button with a circular progress ring at the upper left; the lyrics remain visible beneath it, and tapping the artwork reopens the playback controls. On the lyrics page, the floating Home/Settings bar hides after a period of inactivity and reappears when the lyrics are scrolled. Playback controls and secondary status text use an accent extracted from the current album artwork, independently of the Monet app theme; the song title and artist retain their normal text colors. The lyric appearance settings allow importing a custom TTF or OTF font (up to 20 MB) and restoring the default font. In dark mode, lyric highlight colors are automatically brightened as needed for contrast and inactive lyrics fade less. The Settings theme card supports following the system appearance, forcing light mode, or forcing dark mode; select the Monet color circle to use the device wallpaper's dynamic primary color, or choose a manual palette. The playback cache manager combines the audio-cache limit control and a per-song view of audio and lyric cache entries. It also offers a confirmed one-tap action to clear all playback and lyric cache files. Playback audio cache has a user-adjustable 0.1–20 GB limit and evicts least-recently-used, unpinned songs when it exceeds the limit. Lyrics successfully fetched from Apple Music or QQ Music are persisted in the app's private files, associated with the Apple Music song ID, and reused on later visits to the same song; the lyric cache is source-specific and retains up to 500 entries. Apple Music word-timed lyrics are preferred; otherwise the app tries matching QQ Music QRC lyrics, then the existing online lyric sources. QQ searches use the localized title and artist alongside the original metadata. QQ lyrics use HyperLyrics-Enhanced's `GetPlayLyricInfo` flow: QRC, translation, and romanization are requested together; encrypted lyric text is read from the response's `lyric` field and decrypted as QRC, preserving word timing. The original QRC is parsed first and remains usable when the optional translation is empty or invalid. Separate translation lines are matched one-to-one to original lines by start time within 1 second. No legacy `lyric_download.fcg` request or inferred pairing of interleaved languages is used. When QQ returns a separate translation, it is shown by default and can be toggled; otherwise the original lyric remains visible and translation display stays off. The lyric download menu can also export the decrypted QQ QRC and translation payloads in a `*.qq-qrc.txt` diagnostic file for inspecting source timing/format. QQ candidates are ranked using title, artist, album, and duration; a cross-language top-result fallback still requires a duration difference within 2.5 seconds. Long-press the lyrics to open copy, download, source-selection, import, and display options; use the system Back action to return to search results.

When the lyrics card is collapsed, long-press and drag the floating album-art control to reposition it. Its position is saved for the next time it is shown, and its close-fitting circular progress ring uses the artwork accent color. The lyrics card animates away as the floating control animates in.

When the lyrics page has no touch, control, or lyric-scroll activity for 8 seconds, its playback card automatically collapses into the floating album-art progress control. Touching the lyrics or controls resets the inactivity timer.

In **歌词效果与显示**, adjust **当前播放歌词位置** to move the active lyric up or down within the lyrics viewport. The selected percentage is measured from the top and saved with the other appearance settings.

The **歌词页全屏** switch in Settings controls whether the status bar is hidden while lyrics are open. When enabled, lyrics use the full screen height and a swipe from the top edge can temporarily reveal system bars; the playback card still stays below the camera/status-bar safe area. When disabled, the status bar remains visible and the playback card stays below it.

The QQ QRC DES helper is adapted from [Lyricify-Lyrics-Helper](https://github.com/WXRIW/Lyricify-Lyrics-Helper) and is licensed under Apache-2.0; its license is included at `lyrics-tool/src/main/java/com/applemusic/lyricstool/data/qq/LICENSE-APACHE-2.0.txt`.

## Build

Open this directory in Android Studio, or run:

```powershell
.\gradlew.bat :lyrics-tool:assembleDebug
```

The debug APK is written to `lyrics-tool/build/outputs/apk/debug/lyrics-tool-debug.apk`.

The Android SDK must be configured in Android Studio or through `ANDROID_HOME`/`ANDROID_SDK_ROOT`.

## GitHub Releases

Push a version tag such as `v1.16` to build the release APK and publish it to the repository's
GitHub Releases. Both the release and debug APKs are attached. Tags containing a hyphen, such as
`v1.16-rc1`, are published as prereleases. The release APK is currently unsigned; it cannot be
installed as-is until release signing is configured with a private signing key. The debug APK uses
a temporary CI debug key and cannot update an app signed with a different key.

## Personalized radio probe

After connecting an Apple Music account, open **Settings → 个性电台接口测试**, refresh the
recommendations, and select a station. **开始 Apple 原生电台** requests Apple's initial station
tracks, resolves localized song metadata, and plays them through the existing download, decrypt,
and playback pipeline. Startup prefetches until at least five upcoming tracks are queued (six
including the currently playing track); later, the app tops the queue back up to five upcoming
tracks when it runs low. Manually skipping in the radio also
requests a continuation; if the queue is already exhausted, playback waits for Apple's next track
and then advances to it. The lyrics page follows the track that actually starts playing. The
undocumented endpoint can stop or repeat tracks, so continuous playback depends on Apple's responses.
Long-press the album art in the lyrics playback card and swipe down to open the visible playback
queue. Tap a queued song to play it immediately. While a song is playing, the next queued song's
audio and lyrics are preloaded; swipe right or left inside the rectangular playback card to skip
to the next or previous song. The same swipe gestures work on the compact album art. The artwork
itself toggles playback, and the star
button occupies the former play-button position. individual tracks can also be played or favorited to the Apple Music account using the same favorites
endpoint as the web player. The lyrics playback card initializes
its favorite star from the account's **Favorite Songs** smart playlist; if Apple does not expose
that automatic playlist's tracks endpoint, the app reports the lookup failure and will not guess
that the song is unfavorited.

The app automatically detects and binds the account's Favorite Songs smart playlist from its
library playlists, saves the playlist ID, and reads its tracks for favorite-state matching.
