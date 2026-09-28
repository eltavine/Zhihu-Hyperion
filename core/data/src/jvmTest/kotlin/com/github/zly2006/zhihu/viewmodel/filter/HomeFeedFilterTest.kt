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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.filter.RemoteHistorySync
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.platform.MapSettingsStore
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedFilterTest {
    @Test
    fun foregroundDropsContentThatWasAlreadyOpened() = runTest {
        val database = getContentFilterDatabase(createTempDirectory("home-feed-filter").resolve("filter.db").toFile())
        try {
            val settings = MapSettingsStore()
            val filter = HomeFeedFilter(database, settings, RemoteHistorySync(settings, database.contentOpenEventDao()))
            database.contentOpenEventDao().insert(ContentOpenEvent(contentType = "answer", contentId = "1", openFrom = "home_feed"))

            assertEquals(listOf(card(2)), filter.foreground(listOf(card(1), card(2))))
        } finally {
            database.close()
        }
    }

    private fun card(id: Long) = FeedDisplayItem(
        title = "answer $id",
        summary = null,
        details = "",
        feed = null,
        navDestinationJson = Article(id = id, type = ArticleType.Answer).toFeedDisplayItemNavDestinationJson(),
    )
}
