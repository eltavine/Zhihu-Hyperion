/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
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
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.util.TextDocumentStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryStorageTest {
    @Test
    fun repeatVisitMovesToFrontWithLatestDetailsAndWithoutReadingQueueSource() = runTest {
        val history = HistoryStorage(MemoryDocument())
        history.add(Article(title = "旧标题", type = ArticleType.Answer, id = 1))
        history.add(Question(2))
        history.add(Article(title = "新标题", type = ArticleType.Answer, id = 1, readingQueueSourceId = "home"))

        assertEquals(listOf(Article(type = ArticleType.Answer, id = 1), Question(2)), history.history)
        val latest = history.history.first() as Article
        assertEquals("新标题", latest.title)
        assertNull(latest.readingQueueSourceId)
    }

    @Test
    fun keepsNewestThousandEntries() = runTest {
        val history = HistoryStorage(MemoryDocument())
        (1L..1001L).forEach { history.add(Question(it)) }

        assertEquals(1000, history.history.size)
        assertEquals(Question(1001), history.history.first())
        assertEquals(Question(2), history.history.last())
    }

    @Test
    fun documentStaysOldestFirstLikeExistingHistoryFiles() = runTest {
        val document = MemoryDocument(Json.encodeToString(listOf<NavDestination>(Question(1), Question(2))))
        val history = HistoryStorage(document)
        assertEquals(listOf(Question(2), Question(1)), history.history)

        history.add(Question(3))
        assertEquals(listOf(Question(1), Question(2), Question(3)), Json.decodeFromString<List<NavDestination>>(document.readText()!!))
        assertEquals(listOf(Question(3), Question(2), Question(1)), HistoryStorage(document).history)
    }

    @Test
    fun clearPersistsEmptyHistory() = runTest {
        val document = MemoryDocument()
        val history = HistoryStorage(document)
        history.add(Question(1))
        history.clear()

        assertTrue(history.history.isEmpty())
        assertTrue(HistoryStorage(document).history.isEmpty())
    }

    private class MemoryDocument(
        private var text: String? = null,
    ) : TextDocumentStore {
        override fun readText() = text

        override fun writeText(text: String) {
            this.text = text
        }

        override fun delete() {
            text = null
        }
    }
}
