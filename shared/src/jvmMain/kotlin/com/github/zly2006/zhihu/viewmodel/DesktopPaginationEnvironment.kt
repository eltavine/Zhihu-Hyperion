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
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import io.ktor.client.HttpClient
import org.koin.mp.KoinPlatform

class DesktopPaginationEnvironment : PaginationEnvironment {
    private val store = KoinPlatform.getKoin().get<ZhihuAccountStore>()
    private val settingsStore: SettingsStore = KoinPlatform.getKoin().get()
    private val contentFilterDb: ContentFilterDatabase = KoinPlatform.getKoin().get()

    override fun httpClient(): HttpClient = store.client.httpClient()

    override fun xsrfToken(): String = store.session.cookies["_xsrf"] ?: ""

    override fun authenticatedCookies(): Map<String, String> = store.session.cookies

    override suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = store.client.withAuthenticatedClient(block)

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        Log.e(tag ?: "PaginationViewModel", "Failed to fetch feeds", error)
    }
}
