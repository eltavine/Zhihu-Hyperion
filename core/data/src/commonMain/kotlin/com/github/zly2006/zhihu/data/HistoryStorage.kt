/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2026, eltavine <me@eltavine.com>
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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.withReadingQueueSource
import com.github.zly2006.zhihu.util.TextDocumentStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

private const val HISTORY_LIMIT = 1000

/**
 * 本地浏览历史：最近访问的在前，按目的地身份去重，最多保留 [HISTORY_LIMIT] 条。
 *
 * 文档按访问先后排列（旧的在前），与各平台既有的 history.json 兼容。进程内只能共享一个实例，
 * 多个实例各持一份内存副本时，后保存的会覆盖先保存的记录。
 */
class HistoryStorage(
    private val store: TextDocumentStore,
) {
    private val writes = Mutex()

    @Volatile
    var history: List<NavDestination> = runCatching {
        store.readText()?.let { Json.decodeFromString<List<NavDestination>>(it) }.orEmpty()
    }.getOrDefault(emptyList())
        .map { it.withReadingQueueSource(null) }
        .distinct()
        .asReversed()
        private set

    /** 阅读队列来源只描述本次打开方式，不进入历史。 */
    suspend fun add(destination: NavDestination) {
        val entry = destination.withReadingQueueSource(null)
        update { listOf(entry) + (it - entry).take(HISTORY_LIMIT - 1) }
    }

    suspend fun clear() = update { emptyList() }

    private suspend fun update(transform: (List<NavDestination>) -> List<NavDestination>) = writes.withLock {
        history = transform(history)
        val text = Json.encodeToString(history.asReversed())
        withContext(Dispatchers.IO) { store.writeText(text) }
    }
}
