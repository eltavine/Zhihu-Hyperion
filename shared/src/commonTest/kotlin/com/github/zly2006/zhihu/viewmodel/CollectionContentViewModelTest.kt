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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionContentViewModelTest {
    /** 收藏夹链接可能指向已删除的收藏夹，页面要显示知乎给出的原因，而不是崩溃。 */
    @Test
    fun missingCollectionReportsTheErrorInsteadOfThrowing() = runTest {
        val viewModel = openCollection { _ ->
            HttpStatusCode.NotFound to """{"error":{"code":4041,"name":"NotFoundError","message":"资源不存在"}}"""
        }

        assertNull(viewModel.collection)
        assertEquals("资源不存在", viewModel.apiError?.message)
    }

    /** 未登录时收藏夹信息可以读取，条目列表被知乎以 HTTP 401 拒绝；页面要提供登录入口，而不是只能重试。 */
    @Test
    fun guestSeesTheCollectionAndALoginEntryForItsItems() = runTest {
        val viewModel = openCollection { path ->
            if (path.endsWith("/items")) {
                HttpStatusCode.Unauthorized to """{"error":{"code":101,"name":"AuthenticationError","message":"身份未经过验证"}}"""
            } else {
                HttpStatusCode.OK to """{"collection":{"id":19928423,"title":"赞同超过1000的回答","is_public":true,"item_count":70701}}"""
            }
        }

        assertEquals("赞同超过1000的回答", viewModel.title)
        assertEquals("身份未经过验证", viewModel.apiError?.message)
        assertTrue(viewModel.apiError!!.needLogin)
    }

    /** 用知乎对 [response] 给出的状态码和响应体打开收藏夹页，等它的请求全部结束。 */
    private suspend fun TestScope.openCollection(
        response: (path: String) -> Pair<HttpStatusCode, String>,
    ): CollectionContentViewModel {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val client = HttpClient(MockEngine) {
            engine {
                this.dispatcher = dispatcher
                addHandler { request ->
                    val (status, body) = response(request.url.encodedPath)
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            installZhihuCommonClientConfig(mutableMapOf(), "test")
        }
        val environment = object : ZhihuApiEnvironment {
            override fun httpClient() = client

            override fun authenticatedCookies() = mapOf("d_c0" to "device", "_xsrf" to "xsrf")

            override fun xsrfToken() = "xsrf"

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        try {
            val viewModel = CollectionContentViewModel("19928423")
            viewModel.refresh(environment)
            advanceUntilIdle()
            return viewModel
        } finally {
            Dispatchers.resetMain()
            client.close()
        }
    }
}
