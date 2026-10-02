/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Co-author: eltavine <me@eltavine.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.platform.SettingsStore
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** 下拉刷新进行中为 true，此时列表底部不再重复显示加载指示器。 */
val LocalPullToRefreshInProgress = compositionLocalOf { false }

val ProgressIndicatorFooter: @Composable (LazyListState) -> Unit = { state ->
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!LocalPullToRefreshInProgress.current) {
            AppLoadingIndicator()
        }
    }
}

private const val DUPLICATE_KEY_PREFIX = "PaginatedListDuplicateKey"
const val PAGINATED_LIST_RETRY_TAG = "paginated_list_retry"

internal fun <T> uniquePaginatedListKeys(
    items: List<T>,
    key: (T) -> Any,
): List<Any> {
    val originalKeys = items.map(key)
    val reservedKeys = originalKeys.toSet()
    val usedKeys = mutableSetOf<Any>()
    val duplicateOccurrences = mutableMapOf<Any, Int>()

    return originalKeys.map { original ->
        if (usedKeys.add(original)) {
            original
        } else {
            val occurrence = duplicateOccurrences.getOrElse(original) { 1 }
            duplicateOccurrences[original] = occurrence + 1
            uniqueDuplicatePaginatedListKey(original, occurrence, reservedKeys, usedKeys)
        }
    }
}

private fun uniqueDuplicatePaginatedListKey(
    original: Any,
    occurrence: Int,
    reservedKeys: Set<Any>,
    usedKeys: MutableSet<Any>,
): Any {
    var collision = 0
    while (true) {
        val candidate = "$DUPLICATE_KEY_PREFIX:$occurrence:$collision:$original"
        if (candidate !in reservedKeys && usedKeys.add(candidate)) {
            return candidate
        }
        collision++
    }
}

@Composable
private fun <T> LazyItemScope.PaginatedListItem(
    item: T,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    Box(
        modifier = Modifier.animateItem(
            fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
            placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
        ),
    ) {
        itemContent(item)
    }
}

@Composable
fun <T> PaginatedList(
    items: List<T>,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
    reverseLayout: Boolean = false,
    isEnd: () -> Boolean = { false },
    footer: @Composable ((LazyListState) -> Unit)? = null,
    key: ((T) -> Any)? = null,
    topContent: LazyListScope.() -> Unit = {},
    bottomContent: LazyListScope.() -> Unit = {},
    /** 为 true 时列表底部不显示 [footer]，改为“加载失败”和 [onRetry] 对应的“重试”按钮。 */
    loadFailed: Boolean = false,
    onRetry: (() -> Unit)? = null,
    /** 已经加载完却一条也没有时代替“已经到底啦”显示。 */
    emptyContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    val settings = koinInject<SettingsStore>()
    val showContentEndMarker by rememberObservedSetting(settings, PREF_SHOW_CONTENT_END_MARKER) {
        getBoolean(PREF_SHOW_CONTENT_END_MARKER, DEFAULT_SHOW_CONTENT_END_MARKER)
    }
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
            }
        }
    }

    LaunchedEffect(shouldLoadMore, items.size, isEnd()) {
        if (shouldLoadMore && items.isNotEmpty() && !isEnd()) {
            onLoadMore()
            // 留一帧给调用方发布新数据或标记列表结束。
            delay(50)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
    ) {
        topContent(this)

        if (key != null) {
            val itemKeys = uniquePaginatedListKeys(items, key)
            itemsIndexed(items, key = { index, _ -> itemKeys[index] }) { _, item ->
                PaginatedListItem(item, itemContent)
            }
        } else {
            items(items) { item ->
                PaginatedListItem(item, itemContent)
            }
        }

        bottomContent(this)

        item {
            if (loadFailed) {
                EmptyState(
                    icon = AppIcons.Error,
                    title = "加载失败",
                    modifier = Modifier.fillMaxWidth(),
                    action = onRetry?.let { EmptyStateAction("重试", AppIcons.Refresh, PAGINATED_LIST_RETRY_TAG, it) },
                )
            } else if (isEnd() && items.isEmpty() && emptyContent != null) {
                emptyContent()
            } else if (isEnd()) {
                if (!showContentEndMarker) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "已经到底啦",
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                footer?.invoke(listState)
            }
        }
        if (isEnd() && items.isNotEmpty() && showContentEndMarker) {
            pageTurnContentEndMarker()
        }
    }
}
