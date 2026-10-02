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

import com.github.zly2006.zhihu.platform.MapSettingsStore
import com.github.zly2006.zhihu.viewmodel.feed.QuestionFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.FakeBlockedUserDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Leaving a screen cancels the coroutines it launched; that must not surface as "加载失败". */
class LeftCompositionCancellationTest {
    private val requestStarted = CompletableDeferred<Unit>()
    private val hangingClient = HttpClient(
        MockEngine {
            requestStarted.complete(Unit)
            awaitCancellation()
        },
    )
    private val reportedFailures = mutableListOf<Exception>()
    private val environment = object : ZhihuApiEnvironment {
        override fun httpClient() = hangingClient

        override fun authenticatedCookies() = mapOf("d_c0" to "device", "z_c0" to "token", "_xsrf" to "xsrf")

        override fun xsrfToken() = "xsrf"

        override suspend fun handleFetchFailure(tag: String?, error: Exception) {
            reportedFailures += error
        }
    }

    /** Cancels [block] mid-request the way Compose cancels a screen's coroutines when the screen leaves. */
    private suspend fun leaveCompositionDuringRequest(block: suspend () -> Unit) = coroutineScope {
        val job = launch { block() }
        requestStarted.await()
        job.cancel(CancellationException("The coroutine scope left the composition"))
        job.join()
    }

    @Test
    fun leavingTheQuestionWhileTheFollowRequestRunsReportsNoFailure() = runTest {
        val viewModel = QuestionFeedViewModel(1L, MapSettingsStore(), FakeBlockedUserDao())

        leaveCompositionDuringRequest { viewModel.followQuestion(environment, follow = true) }

        assertEquals(emptyList(), reportedFailures)
    }

    @Test
    fun leavingTheDailyPageWhileItLoadsLeavesNoError() = runTest {
        val viewModel = DailyViewModel()

        leaveCompositionDuringRequest { viewModel.loadLatest(hangingClient) }

        assertNull(viewModel.error)
    }
}
