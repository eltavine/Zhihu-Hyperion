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

import com.github.zly2006.zhihu.data.executeZhihuAuthenticatedRequest
import com.github.zly2006.zhihu.data.fetchZhihuAuthenticatedJson
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** 带当前账户凭据访问知乎 Web API 的能力；页面 environment 与各业务协议共享同一套签名与刷新流程。 */
interface ZhihuApiEnvironment {
    fun httpClient(): HttpClient

    fun authenticatedCookies(): Map<String, String>

    suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = block(httpClient(), authenticatedCookies())

    suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? = withAuthenticatedClient { client, cookies ->
        fetchZhihuAuthenticatedJson(client, url) {
            method = HttpMethod.Get
            url {
                protocol = URLProtocol.HTTPS
                if (include.isNotEmpty()) {
                    parameters["include"] = include
                }
            }
            signZhihuFetchRequest(cookies)
        }
    }

    suspend fun signedGetText(url: String): String = withAuthenticatedClient { client, cookies ->
        executeZhihuAuthenticatedRequest(client, url) {
            method = HttpMethod.Get
            signZhihuFetchRequest(cookies)
        }.bodyAsText()
    }

    suspend fun refreshToken() {
        val client = httpClient()
        ZhihuCredentialRefresher.refreshZhihuToken(
            ZhihuCredentialRefresher.fetchRefreshToken(client),
            client,
        )
    }

    suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    )

    fun xsrfToken(): String = ""

    /** 向用户提示一条失败信息；没有界面提示渠道的实现只记录日志。 */
    fun showFailureMessage(message: String) {
        Log.w("ZhihuApiEnvironment", message)
    }

    fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        Log.e(tag ?: "PaginationViewModel", "Failed to decode item: $item", error)
    }
}
