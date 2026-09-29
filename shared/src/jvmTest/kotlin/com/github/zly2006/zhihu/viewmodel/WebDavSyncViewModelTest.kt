/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
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

import androidx.room.Room
import com.github.zly2006.zhihu.data.ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.QUALITY_FILTER_MODE_PREFERENCE_KEY
import com.github.zly2006.zhihu.platform.MapSettingsStore
import com.github.zly2006.zhihu.ui.components.ANSWER_SWITCH_SENSITIVITY_PREFERENCE_KEY
import com.github.zly2006.zhihu.util.TextDocumentStore
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.buildContentFilterDatabase
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class WebDavSyncViewModelTest {
    private val database: ContentFilterDatabase = buildContentFilterDatabase(Room.inMemoryDatabaseBuilder<ContentFilterDatabase>())
    private val settings = MapSettingsStore()
    private val requests = mutableListOf<HttpRequestData>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun uploadSendsBlocklistAndOnlyRegisteredSettingsWithBasicAuth() = runBlocking {
        database.blockedKeywordDao().insertKeyword(BlockedKeyword(keyword = "广告"))
        database.blockedUserDao().insertUser(BlockedUser(userId = "user-1", userName = "营销号"))
        settings.putString(QUALITY_FILTER_MODE_PREFERENCE_KEY, "HIDE")
        settings.putInt(ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY, 42)
        settings.putBoolean("showFeedThumbnail", false)
        settings.putString("githubToken", "ghp_secret")
        settings.putBoolean("allowTelemetry", false)
        settings.putString("aigcVoteClientId", "install-id")

        val viewModel = viewModel { request ->
            respond("", if (request.method.value == "MKCOL") HttpStatusCode.Created else HttpStatusCode.NoContent)
        }
        viewModel.upload()
        viewModel.awaitIdle()

        assertEquals("已上传屏蔽列表和 3 项设置", viewModel.status)
        assertEquals(
            listOf(
                "MKCOL https://dav.example.com/dav/zhihu-plus-plus/",
                "PUT https://dav.example.com/dav/zhihu-plus-plus/blocklist.json",
                "PUT https://dav.example.com/dav/zhihu-plus-plus/settings.json",
            ),
            requests.map { "${it.method.value} ${it.url}" },
        )
        requests.forEach {
            assertEquals("Basic bWU6YXBwLXBhc3N3b3Jk", it.headers[HttpHeaders.Authorization])
            assertNull(it.headers[HttpHeaders.Cookie])
        }
        assertTrue("广告" in (requests[1].body as TextContent).text)
        val uploadedBackup = Json.parseToJsonElement((requests[2].body as TextContent).text).jsonObject
        assertEquals(JsonPrimitive(1), uploadedBackup["version"])
        val uploadedSettings = uploadedBackup.getValue("values").jsonObject
        assertEquals(
            JsonObject(
                mapOf(
                    QUALITY_FILTER_MODE_PREFERENCE_KEY to JsonPrimitive("HIDE"),
                    ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY to JsonPrimitive(42),
                    "showFeedThumbnail" to JsonPrimitive(false),
                ),
            ),
            uploadedSettings,
        )
    }

    @Test
    fun restoreMergesBlocklistAndWritesRegisteredSettingsWithTheirTypes() = runBlocking {
        database.blockedKeywordDao().insertKeyword(BlockedKeyword(keyword = "本机已有"))
        val remoteBlocklist = """{"version":3,"exportTime":1,"keywords":[{"keyword":"远端"}],"topics":[{"topicId":"t1","topicName":"话题"}]}"""
        val remoteSettings =
            """
            {"version":1,"exportTime":1,"values":{
              "answerVoteupThreshold":"7",
              "showFeedThumbnail":false,
              "qualityFilterMode":"OFF",
              "answerSwitchSensitivity":1.5,
              "githubToken":"ghp_leaked",
              "themeMode":3
            }}
            """.trimIndent()

        val viewModel = viewModel { request ->
            respond(if (request.url.encodedPath.endsWith("blocklist.json")) remoteBlocklist else remoteSettings)
        }
        viewModel.restore()
        viewModel.awaitIdle()

        assertEquals(
            setOf("本机已有", "远端"),
            database
                .blockedKeywordDao()
                .getAllKeywords()
                .map { it.keyword }
                .toSet(),
        )
        assertEquals(listOf("t1"), database.blockedTopicDao().getAllTopics().map { it.topicId })
        assertEquals(7, settings.getInt(ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY, 0))
        assertFalse(settings.getBoolean("showFeedThumbnail", true))
        assertEquals("OFF", settings.getString(QUALITY_FILTER_MODE_PREFERENCE_KEY, ""))
        assertEquals(1.5f, settings.getFloat(ANSWER_SWITCH_SENSITIVITY_PREFERENCE_KEY, 0f))
        assertFalse(settings.contains("githubToken"))
        assertFalse(settings.contains("themeMode"))
        assertTrue(viewModel.status.orEmpty().contains("4 项设置"), viewModel.status)
    }

    @Test
    fun plainHttpToRemoteHostIsRejectedWithoutSendingCredentials() {
        val viewModel = viewModel(url = "http://dav.example.com/dav/") { respond("") }

        viewModel.testConnection()

        assertEquals("请填写以 https:// 开头的 WebDAV 目录地址", viewModel.status)
        assertTrue(requests.isEmpty())
    }

    private fun viewModel(
        url: String = "https://dav.example.com/dav",
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): WebDavSyncViewModel {
        val engine = MockEngine { request ->
            requests += request
            handler(this, request)
        }
        val configFile = WebDavConfigFile(
            object : TextDocumentStore {
                private var text: String? = null

                override fun readText() = text

                override fun writeText(text: String) {
                    this.text = text
                }

                override fun delete() {
                    text = null
                }
            },
        )
        return WebDavSyncViewModel(settings, engine, database, configFile).apply {
            config = WebDavConfig(url = url, username = "me", password = "app-password")
        }
    }

    private suspend fun WebDavSyncViewModel.awaitIdle() = withTimeout(10.seconds) {
        while (isRunning) delay(10)
    }
}
