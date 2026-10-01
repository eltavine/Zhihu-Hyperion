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

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.withReadingQueueSource
import com.github.zly2006.zhihu.viewmodel.filter.BrowsingHistoryDao
import com.github.zly2006.zhihu.viewmodel.filter.BrowsingHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val HISTORY_LIMIT = 1000

/** 本地浏览历史：最近访问的在前，按目的地身份去重，最多保留 [HISTORY_LIMIT] 条。 */
class HistoryStorage(
    private val dao: BrowsingHistoryDao,
) {
    suspend fun entries(): List<NavDestination> = withContext(Dispatchers.Default) {
        dao.getDestinationsNewestFirst().mapNotNull { json ->
            runCatching { Json.decodeFromString<NavDestination>(json) }.getOrNull()
        }
    }

    /** 阅读队列来源只描述本次打开方式，不进入历史。 */
    suspend fun add(destination: NavDestination) {
        val entry = destination.withReadingQueueSource(null)
        // 可读链接解析出的用户只有 url token，资料加载后才有真实 id；两者相等，替换掉那条占位记录。
        if (entry is Person && entry.id != Person.EMPTY_ID) dao.delete("person:${entry.urlToken}")
        val json = Json.encodeToString<NavDestination>(entry)
        dao.insert(
            BrowsingHistoryEntry(
                // 与 NavDestination 的相等语义一致：同一回答、问题、想法或用户再次访问时替换旧记录。
                destinationKey = when (entry) {
                    is Article -> "${entry.type}:${entry.id}"
                    is Question -> "question:${entry.questionId}"
                    is Pin -> "pin:${entry.id}"
                    is Person -> "person:${entry.id.takeIf { it != Person.EMPTY_ID } ?: entry.urlToken}"
                    else -> json
                },
                destinationJson = json,
            ),
        )
        dao.keepNewest(HISTORY_LIMIT)
    }

    suspend fun clear() = dao.clear()
}
