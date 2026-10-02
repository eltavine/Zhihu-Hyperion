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

package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.zly2006.zhihu.data.DailySection
import com.github.zly2006.zhihu.data.DailyStoriesResponse
import com.github.zly2006.zhihu.data.DailyTopStory
import com.github.zly2006.zhihu.data.fetchDailyStoriesBefore
import com.github.zly2006.zhihu.data.fetchLatestDailyStories
import com.github.zly2006.zhihu.util.Log
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class DailyViewModel : ViewModel() {
    var sections by mutableStateOf<List<DailySection>>(emptyList())
        private set
    var topStories by mutableStateOf<List<DailyTopStory>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set

    /** 首屏加载失败的原因，页面显示在失败状态里。 */
    var error by mutableStateOf<String?>(null)
        private set

    /** 加载更早日期失败的原因，列表底部据此显示失败和重试。 */
    var loadMoreError by mutableStateOf<String?>(null)
        private set
    private var nextDate: String? = null

    /** 最近一次首屏请求的日期，null 表示最新一期；[retry] 重复这次请求。 */
    private var requestedDate: String? = null

    suspend fun loadLatest(httpClient: HttpClient) {
        requestedDate = null
        isLoading = true
        try {
            val data: DailyStoriesResponse = httpClient.fetchLatestDailyStories()
            sections = if (data.stories.isEmpty()) emptyList() else listOf(DailySection(data.date, data.stories))
            topStories = data.topStories
            nextDate = data.date
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "未知错误"
        } finally {
            isLoading = false
        }
    }

    suspend fun retry(httpClient: HttpClient) {
        requestedDate?.let { loadDate(httpClient, it) } ?: loadLatest(httpClient)
    }

    suspend fun loadDate(httpClient: HttpClient, date: String) {
        requestedDate = date
        isLoading = true
        sections = emptyList()
        topStories = emptyList()
        try {
            val nextApiDate = LocalDate
                .parse("${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}")
                .plus(1, DateTimeUnit.DAY)
                .toString()
                .replace("-", "")
            val data: DailyStoriesResponse = httpClient.fetchDailyStoriesBefore(nextApiDate)
            sections = if (data.stories.isEmpty()) emptyList() else listOf(DailySection(data.date, data.stories))
            nextDate = data.date
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "未知错误"
        } finally {
            isLoading = false
        }
    }

    suspend fun loadMore(httpClient: HttpClient) {
        val date = nextDate ?: return
        if (isLoadingMore) return
        isLoadingMore = true
        loadMoreError = null
        try {
            val data: DailyStoriesResponse = httpClient.fetchDailyStoriesBefore(date)
            if (data.stories.isNotEmpty()) {
                sections = sections + DailySection(data.date, data.stories)
            }
            nextDate = data.date
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("DailyViewModel", "Failed to load more daily stories", e)
            loadMoreError = e.message ?: "未知错误"
        } finally {
            isLoadingMore = false
        }
    }
}
