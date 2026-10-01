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

package com.github.zly2006.zhihu.video

import com.github.zly2006.zhihu.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.util.TextDocumentStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class VideoViewModelTest {
    private val requestedUrls = mutableListOf<String>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun playsTheHighestBitrateStreamOfTheRequestedVideo() = runBlocking {
        val state = load(
            HttpStatusCode.OK,
            """
            {
              "playlist": {
                "LD": {"play_url": "https://vdn.example.com/ld.mp4", "bitrate": 206.0, "width": 848},
                "FHD": {"play_url": "https://vdn.example.com/fhd.mp4", "bitrate": 398.0, "width": 1920},
                "HD": {"play_url": "https://vdn.example.com/hd.mp4", "bitrate": 306.0, "width": 1280}
              },
              "watermarked": 0,
              "cover_url": "https://pic.example.com/cover.jpg",
              "playlist_v2": null,
              "title": null
            }
            """.trimIndent(),
        )

        assertEquals(listOf("https://lens.zhihu.com/api/v4/videos/42"), requestedUrls)
        val ready = assertIs<VideoState.Ready>(state)
        assertEquals("https://vdn.example.com/fhd.mp4", ready.playUrl)
        assertEquals("https://pic.example.com/cover.jpg", ready.coverUrl)
    }

    @Test
    fun deletedVideoShowsTheReasonFromLens() = runBlocking {
        val state = load(
            HttpStatusCode.Forbidden,
            """{"error": {"message": "视频不存在", "code": 403, "name": "PermissionDeniedException"}}""",
        )

        assertEquals("视频不存在", assertIs<VideoState.Failed>(state).message)
    }

    @Test
    fun videoWithoutStreamsCannotPlay() = runBlocking {
        val state = load(HttpStatusCode.OK, """{"playlist": {}, "cover_url": null}""")

        assertEquals("视频没有可播放的清晰度", assertIs<VideoState.Failed>(state).message)
    }

    private suspend fun load(
        status: HttpStatusCode,
        body: String,
    ): VideoState {
        val engine = MockEngine { request ->
            requestedUrls += request.url.toString()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = ZhihuAccountStore(ZhihuAccountRepository(MemoryDocument()), engine)
        try {
            val viewModel = VideoViewModel(42, store)
            withTimeout(10.seconds) {
                while (viewModel.state == VideoState.Loading) delay(10)
            }
            return viewModel.state
        } finally {
            store.close()
        }
    }

    private class MemoryDocument : TextDocumentStore {
        private var text: String? = null

        override fun readText() = text

        override fun writeText(text: String) {
            this.text = text
        }

        override fun delete() {
            text = null
        }
    }
}
