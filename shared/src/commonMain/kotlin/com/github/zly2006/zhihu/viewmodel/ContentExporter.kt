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
import io.ktor.client.HttpClient

/** 文章导出的平台能力：存储权限、导出样式资源、HTML 生成与保存、长图渲染与保存。 */
interface ArticleExporter {
    fun hasImageExportPermission(): Boolean

    fun requiresHtmlExportPermission(): Boolean

    fun requestImageExportPermission()

    fun loadExportAssetText(fileName: String): String

    fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String

    suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String

    fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String

    fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    )

    fun articleImageExportRenderer(): ArticleImageExportRenderer
}

/** 收藏夹整体导出为 HTML 压缩包；条目详情通过 [ZhihuApiEnvironment] 补拉。 */
interface CollectionExporter {
    suspend fun exportCollectionItemsToHtmlZip(
        environment: ZhihuApiEnvironment,
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult

    suspend fun handleCollectionExportFailure(error: Exception)
}

/** 各平台的导出实现同时提供两种导出；页面只按需要持有其中一个接口。 */
interface ContentExporter :
    ArticleExporter,
    CollectionExporter

@Composable
expect fun rememberContentExporter(): ContentExporter
