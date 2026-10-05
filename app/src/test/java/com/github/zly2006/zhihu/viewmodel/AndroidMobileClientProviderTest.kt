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

import com.github.zly2006.zhihu.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.account.ZhihuAccountSession
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.platform.MapSettingsStore
import com.github.zly2006.zhihu.util.AtomicTextFile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.io.path.createTempDirectory

class AndroidMobileClientProviderTest {
    /** 关闭“推荐内容时登录”只应影响推荐；通知和私信原来也跟着丢掉登录凭证，显示的是知乎匿名访客的通知。 */
    @Test
    fun onlyRecommendationsDropLoginCookiesWhenRecommendationLoginIsOff() = runBlocking {
        val cookieHeaders = mutableListOf<String?>()
        val accountStore = ZhihuAccountStore(
            ZhihuAccountRepository(AtomicTextFile(Path(createTempDirectory().toString(), "account.json"))),
            MockEngine { respond("") },
        )
        accountStore.replaceSession(ZhihuAccountSession(login = true, cookies = mutableMapOf("z_c0" to "token")))
        val provider = AndroidMobileClientProvider(
            MockEngine { request ->
                cookieHeaders += request.headers[HttpHeaders.Cookie]
                respond("")
            },
            MapSettingsStore().apply { putBoolean("loginForRecommendation", false) },
            accountStore,
        )

        provider.withClient { it.get("https://api.zhihu.com/notifications/v3/message/v3?limit=20") }
        provider.withClient(allowGuestAccess = true) { it.get("https://api.zhihu.com/topstory/recommend") }

        assertEquals(listOf("z_c0=token", null), cookieHeaders)
        accountStore.close()
    }
}
