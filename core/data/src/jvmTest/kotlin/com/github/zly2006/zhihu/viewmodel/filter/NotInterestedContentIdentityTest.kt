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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.toDisplayItem
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotInterestedContentIdentityTest {
    @Test
    fun webAndNavigationBasedRecommendationsShareIdentityDespiteDifferentMetadata() {
        val web = CommonFeed(
            target = Feed.ArticleTarget(
                id = 70,
                url = "https://zhuanlan.zhihu.com/p/70",
                author = Person(id = "author", url = "", userType = "people", name = "作者", headline = "", avatarUrl = ""),
                title = "原始标题",
            ),
        ).toDisplayItem(enableQualityFilter = false)
        val mobileOrLocal = FeedDisplayItem(
            title = "更新后的标题",
            summary = "摘要也不同",
            details = "不同推荐来源",
            feed = null,
            navDestinationJson = Article(type = ArticleType.Article, id = 70, title = "更新后的标题").toFeedDisplayItemNavDestinationJson(),
        )
        assertEquals("article:70", web.notInterestedContentKey())
        assertEquals(web.notInterestedContentKey(), mobileOrLocal.notInterestedContentKey())
    }

    @Test
    fun sameNumericIdKeepsContentTypesSeparateAndPlaceholderCannotBeDismissed() {
        val item = FeedDisplayItem("占位", null, "", null)
        assertNull(item.notInterestedContentKey())
        val destinations = listOf(
            Article(type = ArticleType.Answer, id = 70),
            Article(type = ArticleType.Article, id = 70),
            Question(questionId = 70),
            Pin(id = 70),
            Video(id = 70),
        )
        assertEquals(
            listOf("answer:70", "article:70", "question:70", "pin:70", "video:70"),
            destinations.map { item.copy(navDestinationJson = it.toFeedDisplayItemNavDestinationJson()).notInterestedContentKey() },
        )
    }
}
