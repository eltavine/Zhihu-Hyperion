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

package com.github.zly2006.zhihu.viewmodel.local

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** [recommendationEngine] 在首次加载时才解析，数据库初始化失败会走下方的兜底内容与错误提示。 */
class LocalHomeFeedViewModel(
    settings: SettingsStore,
    private val recommendationEngine: Lazy<LocalRecommendationEngine>,
) : BaseFeedViewModel(settings),
    HomeFeedInteractionViewModel {
    private val recommendationResults = mutableMapOf<String, CrawlingResult>()

    /** Room 生成代码缺失时本地推荐无法工作，首页据此提示用户重启或清除数据。 */
    var showDatabaseError by mutableStateOf(false)

    override val initialUrl: String
        get() = error("LocalHomeFeedViewModel should not be used directly. Use LocalFeedViewModel instead.")

    override fun loadMore(environment: ZhihuApiEnvironment) {
        if (displayItems.isEmpty()) {
            super.loadMore(environment)
        }
    }

    override suspend fun fetchFeeds(environment: ZhihuApiEnvironment) {
        try {
            val engine = recommendationEngine.value.also { it.initialize() }
            val recommendations = engine.generateRecommendations(20)
            recommendationResults.clear()

            if (recommendations.isEmpty()) {
                generateFallbackContent()
            } else {
                val loadedItems = recommendations.map { entry ->
                    FeedDisplayItem(
                        title = entry.feed.title,
                        summary = entry.feed.summary,
                        details = entry.feed.reasonDisplay,
                        feed = null,
                        navDestinationJson = entry.navDestination?.toFeedDisplayItemNavDestinationJson(),
                        isFiltered = false,
                    ).also { item ->
                        recommendationResults[item.stableKey] = entry.result
                    }
                }
                addDisplayItems(loadedItems)
                latestLoadedDisplayItems.value = loadedItems
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("LocalHomeFeedViewModel", "Error fetching local feeds", e)
            if (e.message?.contains("does not exist. Is Room annotation processor correctly configured?") == true) {
                showDatabaseError = true
            }
            generateFallbackContent()
        } finally {
            isLoading = false
        }
    }

    fun onLocalItemOpened(item: FeedDisplayItem) {
        val result = recommendationResults[item.stableKey] ?: return
        if (!recommendationEngine.isInitialized()) {
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            recommendationEngine.value.recordContentOpened(result.contentId, result.reason)
        }
    }

    private suspend fun generateFallbackContent() {
        val fallbackItems = listOf(
            FeedDisplayItem(
                title = "本地推荐正在建立候选池",
                summary = "系统会先抓取关注动态、热门内容和相关话题，再根据你的点击逐步调整排序。",
                details = "本地推荐 · 冷启动",
                feed = null,
                isFiltered = false,
            ),
            FeedDisplayItem(
                title = "你的行为只在本地学习",
                summary = "点开内容会影响后续排序，但这些学习信号不会作为推荐特征上传到服务器。",
                details = "本地推荐 · 隐私优先",
                feed = null,
                isFiltered = false,
            ),
        )

        fallbackItems.forEach { item ->
            if (displayItems.none { existing -> existing.stableKey == item.stableKey }) {
                displayItems.add(item)
            }
            delay(300)
        }
        latestLoadedDisplayItems.value = fallbackItems
    }

    override suspend fun recordContentInteraction(
        environment: ZhihuApiEnvironment,
        feed: Feed,
    ) = Unit

    override fun onUiContentClick(environment: ZhihuApiEnvironment, feed: Feed, item: FeedDisplayItem) = Unit
}
