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

package com.github.zly2006.zhihu.viewmodel.feed

import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.filter.RemoteHistorySync
import com.github.zly2006.zhihu.platform.MapSettingsStore
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.HomeFeedFilter
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.net.UnknownHostException
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

/** https://github.com/eltavine/Zhihu-Hyperion/issues/33 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeFeedReadReportTest {
    /** 点开首页内容时会向知乎上报已读；断网时这次上报失败，不能变成未捕获的异常让应用崩溃。 */
    @Test
    fun readReportFailingOfflineDoesNotCrash() = runTest {
        var reportAttempts = 0
        val client = HttpClient(
            MockEngine { request ->
                reportAttempts++
                throw UnknownHostException("Unable to resolve host \"${request.url.host}\": No address associated with hostname")
            },
        ) {
            installZhihuCommonClientConfig(mutableMapOf(), "test")
        }
        val environment = object : ZhihuApiEnvironment {
            override fun httpClient() = client

            override fun authenticatedCookies() = mapOf("d_c0" to "device", "z_c0" to "token", "_xsrf" to "xsrf")

            override fun xsrfToken() = "xsrf"

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val settings = MapSettingsStore()
        val database = getContentFilterDatabase(createTempDirectory("home-feed-read-report").resolve("filter.db").toFile())
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel = HomeFeedViewModel(
                settings,
                HomeFeedFilter(database, settings, RemoteHistorySync(settings, database.contentOpenEventDao())),
            )
            val feed = CommonFeed(
                id = "1994042228209911642",
                verb = "TOPSTORY_RECOMMEND",
                target = Feed.AnswerTarget(
                    id = 1994042228209911642,
                    url = "https://www.zhihu.com/question/1967520171833927274/answer/1994042228209911642",
                    author = Person(
                        id = "author",
                        url = "https://www.zhihu.com/people/author",
                        userType = "people",
                        name = "作者",
                        headline = "",
                        avatarUrl = "",
                    ),
                    question = Feed.QuestionTarget(
                        id = 1967520171833927274,
                        _title = "问题",
                        url = "https://www.zhihu.com/question/1967520171833927274",
                        type = "question",
                    ),
                ),
            )

            viewModel.onUiContentClick(environment, feed, FeedDisplayItem(title = "问题", summary = null, details = "", feed = feed))
            viewModel.viewModelScope.coroutineContext.job.children
                .toList()
                .forEach { it.join() }

            assertEquals(1, reportAttempts)
        } finally {
            Dispatchers.resetMain()
            client.close()
            database.close()
        }
    }
}
