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

import androidx.room.useWriterConnection
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BrowsingHistoryEntry
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryStorageTest {
    private val databaseFile = createTempDirectory("history").resolve("filter.db").toFile()

    @Test
    fun repeatVisitMovesToFrontWithLatestDetailsAndWithoutReadingQueueSource() = runTest {
        withHistory { history ->
            history.add(Article(title = "旧标题", type = ArticleType.Answer, id = 1))
            history.add(Question(2))
            history.add(Article(title = "新标题", type = ArticleType.Answer, id = 1, readingQueueSourceId = "home"))

            val entries = history.entries()
            assertEquals(listOf(Article(type = ArticleType.Answer, id = 1), Question(2)), entries)
            val latest = entries.first() as Article
            assertEquals("新标题", latest.title)
            assertNull(latest.readingQueueSourceId)
        }
    }

    @Test
    fun keepsNewestThousandEntries() = runTest {
        withHistory { history ->
            (1L..1001L).forEach { history.add(Question(it)) }

            val entries = history.entries()
            assertEquals(1000, entries.size)
            assertEquals(Question(1001), entries.first())
            assertEquals(Question(2), entries.last())
        }
    }

    @Test
    fun loadedProfileReplacesThePeopleOpenedFromLinks() = runTest {
        withHistory { history ->
            val id = "0123456789abcdef0123456789abcdef"
            history.add(Person(id = Person.EMPTY_ID, urlToken = "reader"))
            history.add(Person(id = id, urlToken = id))
            history.add(Person(id = id, urlToken = "reader", name = "读者"))

            val person = history.entries().single() as Person
            assertEquals("reader", person.urlToken)
            assertEquals("读者", person.name)
        }
    }

    @Test
    fun historySurvivesReopeningAndClearEmptiesIt() = runTest {
        withHistory { it.add(Question(1)) }
        withHistory { history ->
            assertEquals(listOf(Question(1)), history.entries())
            history.clear()
        }
        withHistory { assertTrue(it.entries().isEmpty()) }
    }

    @Test
    fun upgradingAVersion7DatabaseKeepsExistingDataAndStartsAnEmptyHistory() = runTest {
        withDatabase { database ->
            database.blockedKeywordDao().insertKeyword(BlockedKeyword(keyword = "保留"))
            database.useWriterConnection { connection ->
                listOf("DROP TABLE ${BrowsingHistoryEntry.TABLE_NAME}", "PRAGMA user_version = 7").forEach { sql ->
                    connection.usePrepared(sql) { it.step() }
                }
            }
        }

        withDatabase { database ->
            assertEquals(listOf("保留"), database.blockedKeywordDao().getAllKeywords().map { it.keyword })
            val history = HistoryStorage(database.browsingHistoryDao())
            assertTrue(history.entries().isEmpty())
            history.add(Question(1))
            assertEquals(listOf(Question(1)), history.entries())
        }
    }

    private suspend fun withHistory(block: suspend (HistoryStorage) -> Unit) = withDatabase { block(HistoryStorage(it.browsingHistoryDao())) }

    private suspend fun withDatabase(block: suspend (ContentFilterDatabase) -> Unit) {
        val database = getContentFilterDatabase(databaseFile)
        try {
            block(database)
        } finally {
            database.close()
        }
    }
}
