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

import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyViewModelTest {
    private val requestedUrls = mutableListOf<String>()
    private var offline = false
    private val client = HttpClient(
        MockEngine { request ->
            requestedUrls += request.url.toString()
            if (offline) error("connection closed")
            val date = request.url.encodedPath
                .substringAfter("/before/", "")
                .takeIf { it.isNotEmpty() }
                ?.let { (it.toInt() - 1).toString() }
                ?: "20261002"
            respond(
                """{"date":"$date","stories":[{"id":${date.toLong()},"title":"日报","url":"https://daily.zhihu.com/story/$date","hint":"作者","images":[],"type":0}]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        installZhihuCommonClientConfig(mutableMapOf(), "test")
    }

    /** 选日期后加载失败，“重试”要重新请求那一天，而不是改去加载最新一期。 */
    @Test
    fun retryRepeatsTheDateThatFailed() = runTest {
        val viewModel = DailyViewModel()
        offline = true
        viewModel.loadDate(client, "20261001")
        assertEquals("connection closed", viewModel.error)

        offline = false
        requestedUrls.clear()
        viewModel.retry(client)

        assertEquals(listOf("https://news-at.zhihu.com/api/4/stories/before/20261002"), requestedUrls)
        assertNull(viewModel.error)
        assertEquals("20261001", viewModel.sections.single().date)
    }

    /** 加载更早日期失败要留下原因，列表底部才能显示失败和重试，而不是静默停下。 */
    @Test
    fun loadMoreFailureIsReportedUntilRetrySucceeds() = runTest {
        val viewModel = DailyViewModel()
        viewModel.loadLatest(client)

        offline = true
        viewModel.loadMore(client)
        assertEquals("connection closed", viewModel.loadMoreError)
        assertEquals(1, viewModel.sections.size)

        offline = false
        viewModel.loadMore(client)
        assertNull(viewModel.loadMoreError)
        assertEquals(listOf("20261002", "20261001"), viewModel.sections.map { it.date })
    }
}
