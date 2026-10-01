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

package com.github.zly2006.zhihu.filter

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentOpenTrackerTest {
    private val answer = Article(type = ArticleType.Answer, id = 1)

    @Test
    fun preparedSourceIsHandedToTheSameContentOnlyOnce() {
        val tracker = ContentOpenTracker(database())
        tracker.prepare(answer, ContentOpenFrom.HOME_FEED)
        assertEquals(ContentOpenFrom.UNKNOWN, tracker.consume(Article(type = ArticleType.Answer, id = 2)))
        assertEquals(ContentOpenFrom.HOME_FEED, tracker.consume(answer))
        assertEquals(ContentOpenFrom.UNKNOWN, tracker.consume(answer))

        tracker.prepare(answer, "")
        assertEquals(ContentOpenFrom.UNKNOWN, tracker.consume(answer))
    }

    @Test
    fun recordStoresTheOpenAndConsumesThePreparedSource() = runTest {
        val database = database()
        try {
            val tracker = ContentOpenTracker(database)
            tracker.prepare(answer, ContentOpenFrom.HOME_FEED)
            tracker.record(answer, questionId = 9)

            assertEquals(listOf("answer:1"), database.contentOpenEventDao().getOpenedContentKeysByKeys(listOf("answer:1")))
            assertEquals(ContentOpenFrom.UNKNOWN, tracker.consume(answer))
        } finally {
            database.close()
        }
    }

    private fun database() = getContentFilterDatabase(createTempDirectory("content-open").resolve("filter.db").toFile())
}
