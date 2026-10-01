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
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ScreenZhihuApiEnvironmentTest {
    private val accountClient = HttpClient(MockEngine { respond("") })
    private val account = object : ZhihuApiEnvironment {
        override fun httpClient() = accountClient

        override fun authenticatedCookies() = mapOf("z_c0" to "token", "_xsrf" to "xsrf")

        override fun xsrfToken() = "xsrf"

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
    }
    private val guest = ZhihuGuestClient(MockEngine { respond("") }, "guest-agent")

    @Test
    fun guestClientIsUsedOnlyWhenThePageAllowsItAndLoginForRecommendationIsOff() {
        val settings = MapSettingsStore()
        val homeFeed = ScreenZhihuApiEnvironment(account, guest, settings, allowGuestAccess = true, failures = LoggingFetchFailurePresenter)
        val profile = ScreenZhihuApiEnvironment(account, guest, settings, allowGuestAccess = false, failures = LoggingFetchFailurePresenter)

        assertSame(accountClient, homeFeed.httpClient())
        assertEquals("xsrf", homeFeed.xsrfToken())

        settings.putBoolean("loginForRecommendation", false)
        assertSame(guest.httpClient, homeFeed.httpClient())
        assertEquals(emptyMap(), homeFeed.authenticatedCookies())
        assertEquals("", homeFeed.xsrfToken())
        assertSame(accountClient, profile.httpClient())
        assertEquals("token", profile.authenticatedCookies()["z_c0"])
    }
}
