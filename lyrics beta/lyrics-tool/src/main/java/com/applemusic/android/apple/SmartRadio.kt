package com.applemusic.android.apple

import com.applemusic.android.util.Logx

/**
 * 「智能电台」——把 Apple 的个性化推荐变成**可持续续杯**的流。
 *
 * ## 它解决的问题
 *
 * 之前是点一次电台、一次性排好整个队列（例如 1140 首），放完就循环。
 * 现在改成：**每次只取一小批，快放完时再向 Apple 的推荐要下一批**，
 * 于是听起来就是「一直在放，而且一直有 Apple 推荐的新内容」。
 *
 * ## 素材来源（都不是我们编的，全是 Apple 的产出）
 *
 * 1. 主页「专属推荐歌单」（爱不释耳 / 你的精选集 / 能量充电…）—— Apple 按口味算的；
 * 2. 主页推荐里的**专辑**（每个货架 8~12 张）；
 * 3. 排行榜的专辑 / 歌单。
 *
 * 这些容器被逐个展开成曲目，**去重**后输出；每 2 首推荐插 1 首自己资料库的歌，
 * 保留「自己的歌」那种熟悉感。所有容器都用完后，回到资料库循环（电台本来就该一直放）。
 *
 * ## 仍然不是 Apple 的算法
 *
 * 真正的算法在 `itsradio://` 原生协议后面，HTTP 取不到。这里是**用 Apple 的
 * 推荐结果搭出来的近似**：不学习收听历史、不做相似艺人扩散。
 */
class SmartRadio(private val api: AppleApi) {

    private companion object {
        const val TAG = "SmartRadio"
        @Volatile private var lastRadioOpeningUri = ""

        /**
         * 挡住**明确不是歌曲**的容器 id（歌单 / 电台）。
         *
         * 【教训】一开始我按「`i.` 或纯数字才当歌曲」来过滤，结果**误杀真歌**：
         * Apple 的目录歌曲 id 也可能带 `a.` 前缀 —— 实测
         * `a.6810567505` 是一首真实歌曲（一曲微茫），不是专辑。
         * 所以这里只挡确定是容器的前缀，其余一律放行；
         * 真正播不了的条目由 PlaybackEngine 的「坏条目跳过」兜底，不会卡住队列。
         */
        fun looksLikeSongId(id: String): Boolean {
            if (id.isEmpty()) return false
            if (id.startsWith("pl.")) return false   // 目录歌单
            if (id.startsWith("p.")) return false    // 资料库歌单
            if (id.startsWith("ra.")) return false   // 电台
            if (id.startsWith("r.")) return false    // 电台
            return true
        }

        private fun shelfPriority(title: String): Int {
            val normalized = title.lowercase()
            return when {
                normalized.contains("专属推荐") || normalized.contains("made for you") ||
                        normalized.contains("personalized") -> 100
                normalized.contains("最近播放") || normalized.contains("recently played") -> 80
                normalized.contains("推荐") || normalized.contains("精选") ||
                        normalized.contains("recommended") -> 60
                else -> 20
            }
        }
    }

    /** 已经进过队列的 uri，用于去重。 */
    private val seen = HashSet<String>()

    /** 待展开的容器（歌单 / 专辑），歌单在前、专辑在后。 */
    private val containers = ArrayDeque<ShelfItem>()

    /** 资料库池（打乱），容器用尽后从这里续。 */
    private var libPool = ArrayList<AppleLibrarySync.SongRow>()
    private var libCursor = 0
    private var openingBatchPending = true

    var stationName: String = ""
        private set

    /** 命中电台名的艺人（优先播放）。 */
    var artistHit: String? = null
        private set

    /** 还剩多少容器没展开（用于日志/调试）。 */
    val pendingContainers: Int get() = containers.size

    /**
     * 准备电台素材。
     *
     * @param name    电台名（Apple 常命名为「`<艺人>` & Artists」）
     * @param library 用户资料库全部歌曲
     * @param artists 用户资料库里的艺人名（用于从电台名里认人）
     */
    fun prepare(name: String, library: List<AppleLibrarySync.SongRow>, artists: List<String>) {
        stationName = name
        seen.clear()
        containers.clear()
        libCursor = 0
        openingBatchPending = true

        // ① 从电台名认艺人，命中的排到资料库池最前
        val base = name
            .replace("& Artists", "", ignoreCase = true)
            .replace("’s Station", "", ignoreCase = true)
            .replace("'s Station", "", ignoreCase = true)
            .replace("的电台", "")
            .trim()
        artistHit = if (base.isNotEmpty()) {
            artists.filter { it.isNotEmpty() && (base.contains(it) || it.contains(base)) }
                .maxByOrNull { it.length }
        } else null

        val shuffled = library.shuffled()
        libPool = if (artistHit != null) {
            val hit = shuffled.filter { it.artist.contains(artistHit!!) }
            ArrayList(hit + shuffled.filterNot { it.artist.contains(artistHit!!) })
        } else {
            ArrayList(shuffled)
        }

        // ② 保留 Apple 货架优先级：账号个性推荐在前，排行榜仅作后备。
        val recs = runCatching { api.getRecommendations() }.getOrNull() ?: emptyList()
        val rankedShelves = recs.withIndex()
            .sortedWith(compareByDescending<IndexedValue<Shelf>> { shelfPriority(it.value.title) }
                .thenBy { it.index })
            .map { it.value }
        for (shelf in rankedShelves) {
            containers.addAll(shelf.items.filter { it.isPlaylist || it.isAlbum })
        }

        // ③ 排行榜的专辑 / 歌单（Apple 编辑侧内容）
        val charts = runCatching { api.getCharts() }.getOrNull() ?: emptyList()
        containers.addAll(charts.flatMap { it.items }.filter { it.isAlbum || it.isPlaylist }.shuffled())

        Logx.i(
            TAG,
            "电台「$name」准备就绪：容器 ${containers.size} 个（歌单+专辑+榜单），" +
                    "资料库 ${libPool.size} 首，优先艺人=$artistHit"
        )
    }

    /**
     * 取下一批曲目（供续队列）。
     *
     * @param target 期望条数（可能少给：素材不够时只返回资料库里的）
     */
    fun nextBatch(target: Int): List<AppleLibrarySync.SongRow> {
        val out = ArrayList<AppleLibrarySync.SongRow>(target)
        var guard = 0

        while (out.size < target && containers.isNotEmpty() && guard < 20) {
            val c = containers.removeFirst()
            guard++
            val songs = runCatching {
                if (c.isAlbum) {
                    api.getCatalogAlbumTracks(c.id)
                } else {
                    api.getAnyPlaylistTracks(c.id, 60, 0).items
                }
            }.getOrNull()?.let { orderSongs(it) } ?: continue

            var added = 0
            for (songIndex in songs.indices) {
                val s = songs[songIndex]
                // 【必须过滤】专辑/歌单 id 混进队列会导致播放失败并卡住「下一首」
                if (!looksLikeSongId(s.id)) {
                    Logx.w(TAG, "跳过非歌曲 id：${s.id}（${s.name}）")
                    continue
                }
                val key = "apple:" + s.id
                if (!seen.add(key)) continue
                out.add(AppleLibrarySync.toRow(s))
                added++
                // 每 2 首推荐插 1 首自己的歌，避免整段都是同一个歌单
                if (out.size % 3 == 0) takeLibrary()?.let { out.add(it) }
                if (out.size >= target) {
                    if (songIndex + 1 < songs.size) containers.addFirst(c)
                    break
                }
            }
            if (added == 0) continue
        }

        // 素材不够就用资料库补足（电台本来就该一直放）
        var guard2 = 0
        while (out.size < target && guard2 < target * 4) {
            guard2++
            val r = takeLibrary() ?: break
            out.add(r)
        }

        if (openingBatchPending && out.isNotEmpty()) {
            val previousOpening = lastRadioOpeningUri
            if (previousOpening.isNotEmpty() && out.first().uri == previousOpening) {
                val alternateIndex = out.indexOfFirst { it.uri != previousOpening }
                if (alternateIndex > 0) {
                    val alternate = out.removeAt(alternateIndex)
                    out.add(0, alternate)
                }
            }
            lastRadioOpeningUri = out.first().uri
            openingBatchPending = false
        }

        Logx.i(
            TAG,
            "电台续杯：本批 ${out.size} 首，剩余容器 ${containers.size}，" +
                    "资料库游标 $libCursor/${libPool.size}，开场曲=${if (openingBatchPending) "待定" else out.firstOrNull()?.title ?: "无"}"
        )
        return out
    }

    /** 从资料库池取一首；取完一轮就重新打乱继续（允许重复）。 */
    private fun takeLibrary(): AppleLibrarySync.SongRow? {
        if (libPool.isEmpty()) return null
        if (libCursor >= libPool.size) {
            libPool = ArrayList(libPool.shuffled())
            libCursor = 0
        }
        return libPool[libCursor++]
    }

    private fun orderSongs(songs: List<AppleSong>): List<AppleSong> {
        val shuffled = songs.shuffled()
        val preferredArtist = artistHit ?: return shuffled
        return shuffled.sortedBy {
            if (it.artistName.contains(preferredArtist, ignoreCase = true)) 0 else 1
        }
    }
}
