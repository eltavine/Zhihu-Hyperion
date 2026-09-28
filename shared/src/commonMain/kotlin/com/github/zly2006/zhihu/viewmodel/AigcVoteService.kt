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

import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.data.AIGC_MARKING_ENABLED_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.AigcVoteVoter
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.AIGC_VOTE_SERVER_URL_KEY
import com.github.zly2006.zhihu.util.DEFAULT_AIGC_VOTE_SERVER_URL
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val AIGC_VOTE_CLIENT_ID_KEY = "aigcVoteClientId"

/**
 * 自建 AIGC 标记服务的连接：服务地址、匿名客户端 ID、当前投票人与共享 HttpClient。
 * 只有 [com.github.zly2006.zhihu.platform.isAigcVoteSupported] 的平台会发起请求，client 在首次使用时才创建。
 */
class AigcVoteService(
    private val settings: SettingsStore,
    private val accountStore: ZhihuAccountStore,
    engine: HttpClientEngine,
) : AutoCloseable {
    private val client = lazy {
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(ZhihuJson.json)
            }
        }
    }
    val httpClient: HttpClient by client

    fun isEnabled(): Boolean = settings.getBoolean(AIGC_MARKING_ENABLED_PREFERENCE_KEY, false)

    fun baseUrl(): String = settings
        .getString(AIGC_VOTE_SERVER_URL_KEY, DEFAULT_AIGC_VOTE_SERVER_URL)
        .ifBlank { DEFAULT_AIGC_VOTE_SERVER_URL }

    @OptIn(ExperimentalUuidApi::class)
    fun clientId(): String = settings.getStringOrNull(AIGC_VOTE_CLIENT_ID_KEY)?.takeIf { it.isNotBlank() }
        ?: Uuid.random().toString().also { settings.putString(AIGC_VOTE_CLIENT_ID_KEY, it) }

    fun voter(): AigcVoteVoter? = accountStore.session.self
        ?.let { runCatching { ZhihuJson.decodeJson<Person>(it) }.getOrNull() }
        ?.let { AigcVoteVoter(id = it.id, name = it.name, urlToken = it.urlToken, avatarUrl = it.avatarUrl) }

    override fun close() {
        if (client.isInitialized()) httpClient.close()
    }
}
