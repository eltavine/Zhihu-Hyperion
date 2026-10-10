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

package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.util.ZhihuApiErrorException
import com.github.zly2006.zhihu.util.zhihuApiErrorOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

class ColumnViewModel(
    val columnId: String,
) : PaginationViewModel<Feed.ArticleTarget>(typeOf<Feed.ArticleTarget>()) {
    var detail by mutableStateOf<DataHolder.Column?>(null)
        private set
    var detailFailure by mutableStateOf<ContentLoadFailure?>(null)
        private set
    private var detailJob: Job? = null

    override val initialUrl = "https://www.zhihu.com/api/v4/columns/$columnId/articles?limit=10&offset=0"
    override val include = ""

    val displayItems by derivedStateOf {
        allData.map { article ->
            FeedDisplayItem(
                title = article.title,
                summary = article.excerpt,
                details = article.detailsText,
                feed = null,
                navDestinationJson = article.navDestination?.toFeedDisplayItemNavDestinationJson(),
                avatarSrc = article.author.avatarUrl,
                authorName = article.author.name,
                authorBadgeV2 = article.author.badgeV2,
            )
        }
    }

    suspend fun loadDetail(environment: ZhihuApiEnvironment) {
        detailFailure = null
        try {
            val response = environment.fetchJson(
                "https://www.zhihu.com/api/v4/columns/$columnId",
                "intro,description,articles_count,followers,author,is_following",
            ) ?: error("专栏信息为空")
            response.zhihuApiErrorOrNull()?.let { throw it }
            detail = ZhihuJson.decodeJson<DataHolder.Column>(response)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            detailFailure = ContentLoadFailure(error.message ?: "专栏信息加载失败", (error as? ZhihuApiErrorException)?.needLogin == true)
        }
    }

    override fun refresh(environment: ZhihuApiEnvironment) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch { loadDetail(environment) }
        super.refresh(environment)
    }

    override suspend fun processResponse(environment: ZhihuApiEnvironment, data: List<Feed.ArticleTarget>, rawData: JsonArray) {
        val seen = allData.mapTo(mutableSetOf()) { it.id }
        super.processResponse(environment, data.filter { seen.add(it.id) }, rawData)
    }
}
