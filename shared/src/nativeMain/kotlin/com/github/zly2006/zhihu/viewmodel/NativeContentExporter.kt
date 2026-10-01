/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
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
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.util.Log
import io.ktor.client.HttpClient

/** 原生平台尚未实现导出，界面按 isArticle*ExportSupported 隐藏文章导出入口；误调用时直接失败。 */
private object UnsupportedContentExporter : ContentExporter {
    override fun hasImageExportPermission(): Boolean = unsupported()

    override fun requiresHtmlExportPermission(): Boolean = unsupported()

    override fun requestImageExportPermission() = unsupported()

    override fun loadExportAssetText(fileName: String): String = unsupported()

    override fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = unsupported()

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = unsupported()

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String = unsupported()

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = unsupported()

    override fun articleImageExportRenderer(): ArticleImageExportRenderer = unsupported()

    override suspend fun exportCollectionItemsToHtmlZip(
        environment: ZhihuApiEnvironment,
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult = error("$platformName 暂不支持收藏夹 HTML 压缩包导出")

    override suspend fun handleCollectionExportFailure(error: Exception) {
        Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", error)
    }

    private fun unsupported(): Nothing = error("$platformName 暂不支持文章导出")
}

@Composable
actual fun rememberContentExporter(): ContentExporter = UnsupportedContentExporter
