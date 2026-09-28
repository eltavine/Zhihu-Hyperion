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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.compose.koinInject

/** 页面请求失败时的平台呈现：Android 视情况弹出登录过期或调试对话框并用 Toast 提示，桌面与原生只记录日志。 */
interface FetchFailurePresenter {
    suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    )

    fun showFailureMessage(message: String)
}

internal object LoggingFetchFailurePresenter : FetchFailurePresenter {
    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        Log.e(tag ?: "PaginationViewModel", "Failed to fetch feeds", error)
    }

    override fun showFailureMessage(message: String) {
        Log.w("PaginationViewModel", message)
    }
}

@Composable
expect fun rememberFetchFailurePresenter(): FetchFailurePresenter

/** 不带账户 Cookie 的共享 client，供关闭“推荐需登录”后的访客推荐使用。 */
class ZhihuGuestClient(
    engine: HttpClientEngine,
    userAgent: String,
) : AutoCloseable {
    val httpClient = HttpClient(engine) {
        install(HttpCache)
        install(ContentNegotiation) {
            json(ZhihuJson.json)
        }
        install(UserAgent) {
            agent = userAgent
        }
    }

    override fun close() = httpClient.close()
}

/**
 * 页面级知乎 API：请求沿用 [account] 的当前账户 client 与会话，失败交给 [failures] 呈现。
 * [allowGuestAccess] 的页面在用户关闭“推荐需登录”后改用 [guest]，不携带账户 Cookie。
 */
class ScreenZhihuApiEnvironment(
    private val account: ZhihuApiEnvironment,
    private val guest: ZhihuGuestClient,
    private val settings: SettingsStore,
    private val allowGuestAccess: Boolean,
    private val failures: FetchFailurePresenter,
) : ZhihuApiEnvironment {
    private val guestMode: Boolean
        get() = allowGuestAccess && !settings.getBoolean("loginForRecommendation", true)

    override fun httpClient(): HttpClient = if (guestMode) guest.httpClient else account.httpClient()

    override fun authenticatedCookies(): Map<String, String> = if (guestMode) emptyMap() else account.authenticatedCookies()

    override suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = if (guestMode) block(guest.httpClient, emptyMap()) else account.withAuthenticatedClient(block)

    override fun xsrfToken(): String = if (guestMode) "" else account.xsrfToken()

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) = failures.handleFetchFailure(tag, error)

    override fun showFailureMessage(message: String) = failures.showFailureMessage(message)
}

@Composable
fun rememberZhihuApiEnvironment(allowGuestAccess: Boolean): ZhihuApiEnvironment {
    val account = koinInject<ZhihuApiEnvironment>()
    val guest = koinInject<ZhihuGuestClient>()
    val settings = koinInject<SettingsStore>()
    val failures = rememberFetchFailurePresenter()
    return remember(account, guest, settings, allowGuestAccess, failures) {
        ScreenZhihuApiEnvironment(account, guest, settings, allowGuestAccess, failures)
    }
}
