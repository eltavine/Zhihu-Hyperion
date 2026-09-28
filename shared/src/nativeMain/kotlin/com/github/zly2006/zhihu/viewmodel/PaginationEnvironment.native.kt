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
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import io.ktor.client.HttpClient
import kotlinx.serialization.json.JsonElement
import org.koin.mp.KoinPlatform

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment =
    remember(allowGuestAccess) { NativePaginationEnvironment() }

internal class NativePaginationEnvironment :
    PaginationEnvironment,
    CollectionContentEnvironment {
    private val accountStore = KoinPlatform.getKoin().get<ZhihuAccountStore>()
    private val settingsStore: SettingsStore = KoinPlatform.getKoin().get()
    private val contentFilterDatabase: ContentFilterDatabase = KoinPlatform.getKoin().get()

    override fun httpClient(): HttpClient = accountStore.client.httpClient()

    override fun authenticatedCookies(): Map<String, String> = accountStore.session.cookies

    override suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = accountStore.client.withAuthenticatedClient(block)

    override fun xsrfToken(): String = accountStore.session.cookies["_xsrf"].orEmpty()

    override suspend fun exportCollectionItemsToHtmlZip(
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult = error("$platformName 暂不支持收藏夹 HTML 压缩包导出")

    override suspend fun handleCollectionExportFailure(error: Exception) {
        Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", error)
    }

    override fun logDecodeFailure(tag: String?, item: JsonElement, error: Exception) {
        Log.e(tag ?: "PaginationViewModel", "Failed to decode item: $item", error)
    }

    override suspend fun handleFetchFailure(tag: String?, error: Exception) {
        Log.e(tag ?: "PaginationViewModel", "Failed to fetch feeds", error)
    }
}
