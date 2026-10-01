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

import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.filter.RemoteHistorySync
import com.github.zly2006.zhihu.platform.SettingsStore

/**
 * 首页推荐的本地过滤。前台阶段只查本地与远端同步来的已读、已屏蔽记录，结果立即展示；后台阶段再做质量、
 * 关键词、用户、话题与语义过滤，必要时通过 [ContentDetailProvider] 补拉内容详情。
 *
 * @param semanticMatcher 只有提供语义模型的平台传入；为空时关键词只做字面匹配。
 * @param onNlpBlocked 语义匹配在本轮屏蔽了内容时调用，用于提示用户。
 */
class HomeFeedFilter(
    private val database: ContentFilterDatabase,
    private val settings: SettingsStore,
    private val remoteHistory: RemoteHistorySync,
    private val semanticMatcher: KeywordSemanticMatcher? = null,
    private val onNlpBlocked: suspend (List<FilterableContent>) -> Unit = {},
) {
    suspend fun foreground(items: List<FeedDisplayItem>): List<FeedDisplayItem> {
        remoteHistory.awaitRecentPage()
        return ForegroundReadFilterPipeline(
            settings = settings.toFeedFilterSettings(),
            contentFilterManager = ContentFilterManager(database.contentFilterDao()),
            contentOpenEventDao = database.contentOpenEventDao(),
            blockedFeedRecordDao = database.blockedFeedRecordDao(),
        ).filter(items)
    }

    suspend fun background(
        items: List<FeedDisplayItem>,
        contentDetails: ContentDetailProvider,
    ): List<FeedDisplayItem> {
        val filterSettings = settings.toFeedFilterSettings()
        return FeedDisplayFilterPipeline(
            settings = filterSettings,
            contentDetailProvider = contentDetails,
            contentFilterPipeline = FeedContentFilterPipeline(
                settings = filterSettings,
                blockedKeywordDao = database.blockedKeywordDao(),
                blockedUserDao = database.blockedUserDao(),
                blockedQuestionAuthorDao = database.blockedQuestionAuthorDao(),
                blockedTopicDao = database.blockedTopicDao(),
                blockedKeywordService = BlockedKeywordService(
                    keywordDao = database.blockedKeywordDao(),
                    recordDao = database.blockedContentRecordDao(),
                    semanticMatcher = semanticMatcher,
                ),
                onNlpBlocked = onNlpBlocked,
            ),
            blockedFeedRecordDao = database.blockedFeedRecordDao(),
        ).filter(items)
    }

    /** 点击等交互计入内容过滤的曝光统计。 */
    suspend fun recordInteraction(feed: Feed) {
        if (!settings.toFeedFilterSettings().enableContentFilter) return
        val (type, id) = when (val target = feed.target) {
            is Feed.AnswerTarget -> ContentType.ANSWER to target.id.toString()
            is Feed.ArticleTarget -> ContentType.ARTICLE to target.id.toString()
            is Feed.QuestionTarget -> ContentType.QUESTION to target.id.toString()
            is Feed.PinTarget -> ContentType.PIN to target.id.toString()
            else -> return
        }
        ContentFilterManager(database.contentFilterDao()).recordContentInteraction(type, id)
    }
}
