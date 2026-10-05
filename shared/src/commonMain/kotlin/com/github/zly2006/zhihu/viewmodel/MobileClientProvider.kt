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
import io.ktor.client.HttpClient

/**
 * 借用移动端协议的 HttpClient 执行 [withClient]，client 只在 block 内有效。
 *
 * Android 首页推荐、通知分类与私信只对 Android App 的请求头返回完整数据，这是这些接口专用的窄兼容手段；
 * 没有移动端伪装的平台借用当前账户的 Web client。
 */
interface MobileClientProvider {
    /**
     * @param allowGuestAccess 调用方是否允许匿名请求：只有推荐流传 true，Android 端再按“推荐内容时登录”决定是否带登录凭证；
     *   通知、私信等必须属于当前账号的请求保持 false。
     */
    suspend fun <T> withClient(allowGuestAccess: Boolean = false, block: suspend (HttpClient) -> T): T
}

class AccountWebClientProvider(
    private val accountStore: ZhihuAccountStore,
) : MobileClientProvider {
    override suspend fun <T> withClient(allowGuestAccess: Boolean, block: suspend (HttpClient) -> T): T =
        block(accountStore.client.httpClient())
}
