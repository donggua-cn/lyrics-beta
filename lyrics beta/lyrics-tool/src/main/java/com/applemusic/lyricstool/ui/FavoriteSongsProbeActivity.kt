package com.applemusic.lyricstool.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.applemusic.android.apple.ApplePlaylist
import com.applemusic.android.apple.MusicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteSongsProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FavoriteSongsProbeScreen(onBack = ::finish)
                }
            }
        }
    }
}

@Composable
private fun FavoriteSongsProbeScreen(onBack: () -> Unit) {
    val api = MusicRepository.api
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<ApplePlaylist>>(emptyList()) }
    var trackResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loadingPlaylists by remember { mutableStateOf(false) }
    var testingPlaylistId by remember { mutableStateOf<String?>(null) }
    var configuredPlaylistId by remember { mutableStateOf(api.favoritePlaylistId()) }
    var status by remember { mutableStateOf("点击“读取歌单列表”以检查账号实际返回的资料库歌单。") }

    val loadPlaylists = {
        if (!loadingPlaylists && testingPlaylistId == null) {
            scope.launch {
                loadingPlaylists = true
                status = "正在读取 Apple Music 资料库歌单…"
                try {
                    val loaded = withContext(Dispatchers.IO) {
                        val all = ArrayList<ApplePlaylist>()
                        var offset = 0
                        var page = api.getLibraryPlaylists(100, offset)
                        var pageCount = 0
                        while (page.items.isNotEmpty() && pageCount < 50) {
                            all.addAll(page.items)
                            offset += page.items.size
                            if (page.items.size < 100) break
                            page = api.getLibraryPlaylists(100, offset)
                            pageCount++
                        }
                        all
                    }
                    playlists = loaded
                    trackResults = emptyMap()
                    status = "接口成功，返回 ${loaded.size} 个歌单。请找到名称类似“Favorite Songs / 喜爱歌曲”的项目，点击“测试曲目”。"
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    status = "读取歌单列表失败：${error.message ?: error.javaClass.simpleName}"
                } finally {
                    loadingPlaylists = false
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack) { Text("返回") }
            Button(onClick = loadPlaylists, enabled = !loadingPlaylists && testingPlaylistId == null) {
                Text(if (loadingPlaylists) "读取中…" else "读取歌单列表")
            }
        }
        Text(
            "Favorite Songs 列表诊断",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            if (configuredPlaylistId.isBlank()) {
                "当前未指定喜爱歌曲歌单"
            } else {
                "当前用于收藏状态查询的歌单 ID：$configuredPlaylistId"
            },
            style = MaterialTheme.typography.bodySmall
        )
        Text(status, style = MaterialTheme.typography.bodyMedium)
        if (loadingPlaylists) {
            CircularProgressIndicator()
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(playlists, key = ApplePlaylist::id) { playlist ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(playlist.name.ifBlank { "（无名称）" }, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "ID: ${playlist.id} · 曲目数: ${playlist.trackCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            onClick = {
                                if (testingPlaylistId == null) {
                                    scope.launch {
                                        testingPlaylistId = playlist.id
                                        trackResults = trackResults - playlist.id
                                        try {
                                            val result = withContext(Dispatchers.IO) {
                                                val allTracks = ArrayList<com.applemusic.android.apple.AppleSong>()
                                                var offset = 0
                                                var page = api.getPlaylistTracks(playlist.id, 100, offset)
                                                var pages = 0
                                                val signatures = HashSet<String>()
                                                while (page.items.isNotEmpty() && pages < 100) {
                                                    val pageSignature = page.items.joinToString(",") {
                                                        it.catalogId.ifBlank { it.id }
                                                    }
                                                    if (!signatures.add(pageSignature)) break
                                                    allTracks.addAll(page.items)
                                                    offset += page.items.size
                                                    if (
                                                        page.next.isBlank() &&
                                                        page.total > 0 &&
                                                        offset >= page.total
                                                    ) break
                                                    page = api.getPlaylistTracks(playlist.id, 100, offset)
                                                    pages++
                                                }
                                                allTracks
                                            }
                                            val preview = result.take(10).joinToString("\n") {
                                                "• ${it.name} — ${it.artistName} (${it.catalogId.ifBlank { it.id }})"
                                            }
                                            trackResults = trackResults + (
                                                playlist.id to
                                                    "成功：共读取 ${result.size} 首（包含后续分页）" +
                                                    if (preview.isNotBlank()) "\n$preview" else ""
                                                )
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (error: Exception) {
                                            trackResults = trackResults +
                                                (playlist.id to "读取失败：${error.message ?: error.javaClass.simpleName}")
                                        } finally {
                                            testingPlaylistId = null
                                        }
                                    }
                                }
                            },
                            enabled = testingPlaylistId == null
                        ) {
                            Text(if (testingPlaylistId == playlist.id) "测试中…" else "测试曲目接口")
                        }
                        trackResults[playlist.id]?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        if (trackResults[playlist.id]?.startsWith("成功：") == true) {
                            Button(
                                onClick = {
                                    try {
                                        api.setFavoritePlaylistId(playlist.id)
                                        configuredPlaylistId = playlist.id
                                        status = "已指定“${playlist.name}”作为喜爱歌曲列表。返回歌词页重新打开歌曲即可刷新星标。"
                                    } catch (error: Exception) {
                                        status = "保存歌单设置失败：${error.message ?: error.javaClass.simpleName}"
                                    }
                                }
                            ) {
                                Text(
                                    if (configuredPlaylistId == playlist.id) {
                                        "已设为喜爱歌曲列表"
                                    } else {
                                        "设为喜爱歌曲列表"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
