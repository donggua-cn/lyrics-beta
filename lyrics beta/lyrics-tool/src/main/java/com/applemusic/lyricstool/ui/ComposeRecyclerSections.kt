package com.applemusic.lyricstool.ui

import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

internal data class ComposeRecyclerSection(
    val key: String,
    val viewType: Int,
    val revision: Any?,
    val content: @Composable () -> Unit
)

internal class ComposeRecyclerListState(
    initialLayoutState: Parcelable? = null
) {
    internal var savedLayoutState: Parcelable? = initialLayoutState
    internal var recyclerView: RecyclerView? = null
    internal var pendingScrollPosition: Int? = null
    internal var initialSyncPosted = false

    var firstVisibleItemPosition by mutableIntStateOf(0)
        private set
    var firstVisibleItemKey by mutableStateOf<String?>(null)
        private set
    var lastVisibleItemKey by mutableStateOf<String?>(null)
        private set
    var focusedItemKey by mutableStateOf<String?>(null)
        private set
    var scrollOffsetPx by mutableIntStateOf(0)
        private set
    var isContentScrolled by mutableStateOf(false)
        private set
    var isScrollInProgress by mutableStateOf(false)
        private set

    internal fun updateScroll(
        firstVisiblePosition: Int,
        firstVisibleKey: String?,
        lastVisibleKey: String?,
        focusedKey: String?,
        scrollOffset: Int?,
        contentScrolled: Boolean
    ) {
        firstVisibleItemPosition = firstVisiblePosition.coerceAtLeast(0)
        firstVisibleItemKey = firstVisibleKey
        lastVisibleItemKey = lastVisibleKey
        focusedItemKey = focusedKey
        scrollOffset?.let { scrollOffsetPx = it.coerceAtLeast(0) }
        isContentScrolled = contentScrolled
    }

    internal fun updateScrollState(scrolling: Boolean) {
        isScrollInProgress = scrolling
    }

    fun scrollToPosition(position: Int, smooth: Boolean = true) {
        val view = recyclerView
        if (view == null) {
            pendingScrollPosition = position
            savedLayoutState = null
        } else if (smooth) {
            view.smoothScrollToPosition(position)
        } else {
            view.scrollToPosition(position)
        }
    }

    companion object {
        val Saver: Saver<ComposeRecyclerListState, Parcelable> = Saver(
            save = { it.savedLayoutState },
            restore = { ComposeRecyclerListState(it) }
        )
    }
}

@Composable
internal fun rememberComposeRecyclerListState(): ComposeRecyclerListState =
    rememberSaveable(saver = ComposeRecyclerListState.Saver) {
        ComposeRecyclerListState()
    }

@Composable
internal fun ComposeRecyclerSections(
    sections: List<ComposeRecyclerSection>,
    state: ComposeRecyclerListState,
    saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onScroll: (RecyclerView, Int, Int) -> Unit = { _, _, _ -> },
    onScrollStateChanged: (Boolean) -> Unit = {},
    onPullToRefresh: (() -> Unit)? = null,
    pullToRefreshThresholdPx: Float = 0f,
    trackScrollOffset: Boolean = true,
    contentScrolledThresholdPx: Int = 80
) {
    val density = LocalDensity.current
    val topPadding = (contentPadding.calculateTopPadding().value * density.density).roundToInt()
    val bottomPadding =
        (contentPadding.calculateBottomPadding().value * density.density).roundToInt()
    val parentComposition = rememberCompositionContext()
    val currentOnScroll = rememberUpdatedState(onScroll)
    val currentOnScrollStateChanged = rememberUpdatedState(onScrollStateChanged)
    val currentOnPullToRefresh = rememberUpdatedState(onPullToRefresh)
    val adapter = androidx.compose.runtime.remember(parentComposition, saveableStateHolder) {
        ComposeSectionAdapter(parentComposition, saveableStateHolder)
    }

    AndroidView(
        factory = { viewContext ->
            RecyclerView(viewContext).apply {
                layoutManager = LinearLayoutManager(viewContext)
                itemAnimator = null
                overScrollMode = View.OVER_SCROLL_NEVER
                clipToPadding = false
                setPadding(0, topPadding, 0, bottomPadding)
                this.adapter = adapter
                state.recyclerView = this

                addOnScrollListener(
                    object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(
                            recyclerView: RecyclerView,
                            dx: Int,
                            dy: Int
                        ) {
                            val linearLayout = recyclerView.layoutManager
                                as? LinearLayoutManager ?: return
                            val firstPosition = linearLayout.findFirstVisibleItemPosition()
                            val lastPosition = linearLayout.findLastVisibleItemPosition()
                            val firstView = linearLayout.findViewByPosition(firstPosition)
                            val firstKey = adapter.currentList.getOrNull(firstPosition)?.key
                            val lastKey = adapter.currentList.getOrNull(lastPosition)?.key
                            val viewportCenter = recyclerView.height / 2f
                            var focusedPosition = RecyclerView.NO_POSITION
                            var closestCenterDistance = Float.MAX_VALUE
                            var position = firstPosition
                            while (position <= lastPosition) {
                                val child = linearLayout.findViewByPosition(position)
                                if (child != null) {
                                    val visibleHeight =
                                        minOf(child.bottom, recyclerView.height) -
                                            maxOf(child.top, 0)
                                    if (visibleHeight >= child.height / 2) {
                                        val centerDistance = kotlin.math.abs(
                                            (child.top + child.bottom) / 2f - viewportCenter
                                        )
                                        if (centerDistance < closestCenterDistance) {
                                            closestCenterDistance = centerDistance
                                            focusedPosition = position
                                        }
                                    }
                                }
                                position++
                            }
                            val focusedKey = focusedPosition
                                .takeIf { it != RecyclerView.NO_POSITION }
                                ?.let(adapter.currentList::getOrNull)
                                ?.key
                            state.updateScroll(
                                firstVisiblePosition = firstPosition,
                                firstVisibleKey = firstKey,
                                lastVisibleKey = lastKey,
                                focusedKey = focusedKey,
                                scrollOffset = if (trackScrollOffset) {
                                    recyclerView.computeVerticalScrollOffset()
                                } else {
                                    null
                                },
                                contentScrolled = firstPosition > 0 ||
                                    (firstView?.top ?: 0) <= -contentScrolledThresholdPx
                            )
                            currentOnScroll.value(recyclerView, dx, dy)
                        }

                        override fun onScrollStateChanged(
                            recyclerView: RecyclerView,
                            newState: Int
                        ) {
                            val scrolling = newState != RecyclerView.SCROLL_STATE_IDLE
                            state.updateScrollState(scrolling)
                            currentOnScrollStateChanged.value(scrolling)
                        }
                    }
                )

                var pullStartY = 0f
                var pulledAtTop = false
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            pullStartY = event.y
                            pulledAtTop = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (
                                !canScrollVertically(-1) &&
                                event.y - pullStartY > pullToRefreshThresholdPx
                            ) {
                                pulledAtTop = true
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            if (pulledAtTop) currentOnPullToRefresh.value?.invoke()
                            pulledAtTop = false
                        }
                        MotionEvent.ACTION_CANCEL -> pulledAtTop = false
                    }
                    false
                }
            }
        },
        update = { recyclerView ->
            recyclerView.setPadding(0, topPadding, 0, bottomPadding)
            adapter.submitList(sections) {
                state.pendingScrollPosition?.let { position ->
                    recyclerView.scrollToPosition(position)
                    state.pendingScrollPosition = null
                    state.savedLayoutState = null
                } ?: state.savedLayoutState?.let { savedState ->
                    recyclerView.layoutManager?.onRestoreInstanceState(savedState)
                    state.savedLayoutState = null
                }
                if (!state.initialSyncPosted) {
                    state.initialSyncPosted = true
                    recyclerView.post {
                        if (recyclerView.adapter === adapter) {
                            val linearLayout = recyclerView.layoutManager as? LinearLayoutManager
                            val firstPosition =
                                linearLayout?.findFirstVisibleItemPosition() ?: 0
                            state.updateScroll(
                                firstVisiblePosition = firstPosition,
                                firstVisibleKey = adapter.currentList
                                    .getOrNull(firstPosition)
                                    ?.key,
                                lastVisibleKey = adapter.currentList
                                    .getOrNull(
                                        linearLayout?.findLastVisibleItemPosition()
                                            ?: firstPosition
                                    )
                                    ?.key,
                                focusedKey = adapter.currentList.getOrNull(firstPosition)?.key,
                                scrollOffset = if (trackScrollOffset) {
                                    recyclerView.computeVerticalScrollOffset()
                                } else {
                                    null
                                },
                                contentScrolled = firstPosition > 0 ||
                                    (linearLayout?.findViewByPosition(firstPosition)?.top
                                        ?: 0) <= -contentScrolledThresholdPx
                            )
                            currentOnScroll.value(recyclerView, 0, 0)
                        }
                    }
                }
            }
        },
        onRelease = { recyclerView ->
            state.savedLayoutState = recyclerView.layoutManager?.onSaveInstanceState()
            state.recyclerView = null
            state.initialSyncPosted = false
            recyclerView.clearOnScrollListeners()
            recyclerView.setOnTouchListener(null)
            recyclerView.adapter = null
        },
        modifier = modifier
    )
}

private class ComposeSectionAdapter(
    private val parentComposition: CompositionContext,
    private val saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder
) : androidx.recyclerview.widget.ListAdapter<ComposeRecyclerSection, ComposeSectionHolder>(
    SECTION_DIFF
) {
    init {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeSectionHolder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setParentCompositionContext(parentComposition)
        }
        return ComposeSectionHolder(composeView, saveableStateHolder)
    }

    override fun onBindViewHolder(holder: ComposeSectionHolder, position: Int) {
        holder.section.value = getItem(position)
    }

    override fun onViewRecycled(holder: ComposeSectionHolder) {
        holder.section.value = null
        super.onViewRecycled(holder)
    }

    private companion object {
        val SECTION_DIFF = object : DiffUtil.ItemCallback<ComposeRecyclerSection>() {
            override fun areItemsTheSame(
                oldItem: ComposeRecyclerSection,
                newItem: ComposeRecyclerSection
            ): Boolean = oldItem.key == newItem.key

            override fun areContentsTheSame(
                oldItem: ComposeRecyclerSection,
                newItem: ComposeRecyclerSection
            ): Boolean = oldItem.viewType == newItem.viewType &&
                oldItem.revision == newItem.revision
        }
    }
}

private class ComposeSectionHolder(
    private val composeView: ComposeView,
    private val saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder
) : RecyclerView.ViewHolder(composeView) {
    val section = mutableStateOf<ComposeRecyclerSection?>(null)

    init {
        composeView.setContent {
            section.value?.let { currentSection ->
                key(currentSection.key) {
                    saveableStateHolder.SaveableStateProvider(currentSection.key) {
                        currentSection.content()
                    }
                }
            }
        }
    }
}
