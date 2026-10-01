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

import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.ZhihuCookieStorage
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.platform.SettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendAll

private val ZHIHU_PP_ANDROID_HEADERS = createClientPlugin("ZhihuPPAndroidHeaders", { }) {
    onRequest { request, _ ->
        request.headers.appendAll(AccountData.ANDROID_HEADERS)
    }
}

/** 每次借用都在共享引擎上新建一个带 Android App UA 与请求头的 client，用完即关。 */
class AndroidMobileClientProvider(
    private val engine: HttpClientEngine,
    private val settings: SettingsStore,
    private val accountStore: ZhihuAccountStore,
) : MobileClientProvider {
    override suspend fun <T> withClient(block: suspend (HttpClient) -> T): T = HttpClient(engine) {
        install(ContentNegotiation) {
            json(ZhihuJson.json)
        }
        install(UserAgent) {
            agent = AccountData.ANDROID_USER_AGENT
        }
        install(ZHIHU_PP_ANDROID_HEADERS)
        if (settings.getBoolean("loginForRecommendation", true)) {
            // Mobile responses only update this borrowed copy; the web session stays the single persisted cookie jar.
            install(HttpCookies) {
                storage = ZhihuCookieStorage(accountStore.session.cookies.toMutableMap())
            }
        }
    }.use { block(it) }
}
