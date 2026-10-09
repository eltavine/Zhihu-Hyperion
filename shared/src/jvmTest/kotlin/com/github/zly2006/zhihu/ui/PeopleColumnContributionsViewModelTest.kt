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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleColumnContributionsViewModelTest {
    /**
     * Regression: https://github.com/eltavine/Zhihu-Hyperion/issues/44
     * 按附件日志的 column 嵌套结构模拟两页响应，通过生产请求和解码链验证条目、翻页、到底和刷新。
     */
    @Test
    fun loadsNestedColumnsAndKeepsPagingAndRefreshWorking() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val requests = mutableListOf<String>()
        val decodeFailures = mutableListOf<Exception>()
        val client = HttpClient(MockEngine) {
            engine {
                this.dispatcher = dispatcher
                addHandler { request ->
                    requests += request.url.toString()
                    val secondPage = request.url.parameters["offset"] == "1"
                    respond(
                        """
                        {
                          "data": [{"column": {
                            "id": "${if (secondPage) "c_1308886122672484352" else "c_1865730531905380352"}",
                            "title": "${if (secondPage) "FSRS" else "Paul Graham"}",
                            "articles_count": 14
                          }, "contributions_count": 1}],
                          "paging": {
                            "is_end": $secondPage,
                            "next": "https://www.zhihu.com/api/v4/members/L.M.Sherlock/column-contributions?offset=1"
                          }
                        }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
            installZhihuCommonClientConfig(mutableMapOf(), "test")
        }
        val environment = object : ZhihuApiEnvironment {
            override fun httpClient() = client

            override fun authenticatedCookies() = mapOf("d_c0" to "test-device")

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit

            override fun logDecodeFailure(tag: String?, item: JsonElement, error: Exception) {
                decodeFailures += error
            }
        }
        try {
            val model = PeopleColumnContributionsViewModel(Person("", "L.M.Sherlock"))
            model.loadMore(environment)
            advanceUntilIdle()

            assertEquals(listOf("Paul Graham"), model.allData.map { it.title })
            assertEquals(14, model.allData.single().articlesCount)
            assertFalse(model.isEnd)
            assertFalse(model.isLoading)
            assertNull(model.errorMessage)
            assertTrue(decodeFailures.isEmpty())

            model.loadMore(environment)
            advanceUntilIdle()
            assertEquals(listOf("c_1865730531905380352", "c_1308886122672484352"), model.allData.map { it.id })
            assertTrue(model.isEnd)
            assertFalse(model.isLoading)
            model.loadMore(environment)
            advanceUntilIdle()
            assertEquals(2, requests.size)

            model.refresh(environment)
            advanceUntilIdle()
            assertEquals(listOf("Paul Graham"), model.allData.map { it.title })
            assertFalse(model.isEnd)
            assertEquals(3, requests.size)
            assertTrue(
                model.debugData
                    .first()
                    .toString()
                    .contains("contributions_count"),
            )
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }
}
