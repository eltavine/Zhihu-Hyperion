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

package com.github.zly2006.zhihu.ui.components

import com.github.zly2006.zhihu.ui.components.FeedDetailKind.Collect
import com.github.zly2006.zhihu.ui.components.FeedDetailKind.Comment
import com.github.zly2006.zhihu.ui.components.FeedDetailKind.Recommendation
import com.github.zly2006.zhihu.ui.components.FeedDetailKind.View
import com.github.zly2006.zhihu.ui.components.FeedDetailKind.VoteUp
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedCardDetailsTest {
    // 手机版推荐卡片 footer 的服务端原文（游客请求实测），加上解析器追加的「手机版推荐」。
    @Test
    fun splitsMobileRecommendationFooter() {
        assertEquals(
            listOf(
                FeedDetailPart(VoteUp, "6.7 万", "6.7 万赞同"),
                FeedDetailPart(Collect, "8571", "8571 收藏"),
                FeedDetailPart(Recommendation, "手机版推荐"),
            ),
            feedDetailParts("6.7 万赞同 · 8571 收藏 · 手机版推荐"),
        )
        assertEquals(
            listOf(
                FeedDetailPart(VoteUp, "1637", "1637 赞同"),
                FeedDetailPart(Comment, "636", "636 评论"),
                FeedDetailPart(Recommendation, "手机版推荐"),
            ),
            feedDetailParts("1637 赞同 · 636 评论 · 手机版推荐"),
        )
        assertEquals(
            listOf(FeedDetailPart(View, "4", "4 浏览"), FeedDetailPart(Recommendation, "手机版推荐")),
            feedDetailParts("4 浏览 · 手机版推荐"),
        )
        assertEquals(
            listOf(FeedDetailPart(null, "2小时前"), FeedDetailPart(Recommendation, "手机版推荐")),
            feedDetailParts("2小时前 · 手机版推荐"),
        )
    }

    @Test
    fun keepsTypeAndActionTextOfWebFeedAsPlainText() {
        assertEquals(
            listOf(
                FeedDetailPart(null, "回答"),
                FeedDetailPart(VoteUp, "1234", "1234 赞同"),
                FeedDetailPart(Comment, "56", "56 评论"),
                FeedDetailPart(null, "关注了问题"),
            ),
            feedDetailParts("回答 · 1234 赞同 · 56 评论 · 关注了问题"),
        )
        assertEquals(
            listOf(FeedDetailPart(null, "想法"), FeedDetailPart(VoteUp, "3", "3 赞"), FeedDetailPart(Comment, "0", "0 评论")),
            feedDetailParts("想法 · 3 赞 · 0 评论"),
        )
    }

    @Test
    fun mergesUnrecognizedNeighboursIntoOneText() {
        assertEquals(listOf(FeedDetailPart(null, "问题 · 12 关注 · 3 回答")), feedDetailParts("问题 · 12 关注 · 3 回答"))
        assertEquals(listOf(FeedDetailPart(null, "回答 · -1 赞同 · -1 评论")), feedDetailParts("回答 · -1 赞同 · -1 评论"))
        assertEquals(
            listOf(FeedDetailPart(Recommendation, "本地推荐"), FeedDetailPart(null, "冷启动")),
            feedDetailParts("本地推荐 · 冷启动"),
        )
    }
}
