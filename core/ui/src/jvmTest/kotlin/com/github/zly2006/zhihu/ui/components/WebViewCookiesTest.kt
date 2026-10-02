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

package com.github.zly2006.zhihu.ui.components

import com.github.zly2006.zhihu.account.ZHIHU_HOME_URL
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class WebViewCookiesTest {
    /** 网页登录和风控验证要拿到知乎下发的全部 cookie，包括页面 document.cookie 读不到的 HttpOnly 的 z_c0。 */
    @Test
    fun readsTheSeededAndHttpOnlyCookies() {
        WebViewCookies.startSession(mapOf("d_c0" to "device"))
        WebViewCookies.put(
            URI(ZHIHU_HOME_URL),
            mapOf("Set-Cookie" to listOf("z_c0=\"2|1:0|10:1|4:z_c0|token\"; Domain=.zhihu.com; Path=/; Secure; HttpOnly")),
        )

        assertEquals(mapOf("d_c0" to "device", "z_c0" to "\"2|1:0|10:1|4:z_c0|token\""), WebViewCookies.zhihuCookies())
    }

    /** 每次打开 WebView 都从给定的 cookie 开始，上一次登录留下的会话不能带进下一次。 */
    @Test
    fun newSessionDropsThePreviousCookies() {
        WebViewCookies.startSession(mapOf("z_c0" to "previous-account"))

        WebViewCookies.startSession(emptyMap())

        assertEquals(emptyMap(), WebViewCookies.zhihuCookies())
    }
}
