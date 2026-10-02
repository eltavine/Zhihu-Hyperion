/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Co-author: eltavine <me@eltavine.com>
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

package com.github.zly2006.zhihu.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.platform.androidUserMessageSink
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import java.io.OutputStream

fun buildArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String = buildArticleExportHtml(
    loadAssetText = { fileName -> loadArticleExportAssetText(context, fileName) },
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

fun buildArticleExportHtml(
    context: Context,
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    extraSectionsHtml: String = "",
): String = buildArticleExportHtml(
    loadAssetText = { fileName -> loadArticleExportAssetText(context, fileName) },
    content = content,
    includeAppAttribution = includeAppAttribution,
    extraSectionsHtml = extraSectionsHtml,
)

suspend fun buildOfflineArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
): String = buildOfflineArticleExportHtml(
    loadAssetText = { fileName -> loadArticleExportAssetText(context, fileName) },
    exportData = exportData,
    httpClient = httpClient,
    includeImages = includeImages,
    extraSectionsHtml = extraSectionsHtml,
)

suspend fun buildOfflineArticleExportHtml(
    context: Context,
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
): String = buildOfflineArticleExportHtml(
    loadAssetText = { fileName -> loadArticleExportAssetText(context, fileName) },
    content = content,
    includeAppAttribution = includeAppAttribution,
    httpClient = httpClient,
    includeImages = includeImages,
    extraSectionsHtml = extraSectionsHtml,
)

private fun loadArticleExportAssetText(
    context: Context,
    fileName: String,
): String = context.assets.open(fileName).use { inputStream ->
    inputStream.bufferedReader().use { reader ->
        reader.readText()
    }
}

fun createEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> {
    return emojiKeys
        .filter { it.startsWith("emoji_") }
        .mapNotNull { emojiKey ->
            val fileName = emojiKey.removePrefix("emoji_")
            val path = EmojiManager.getEmojiPathByFileName(fileName) ?: return@mapNotNull null

            emojiKey to InlineTextContent(
                placeholder = Placeholder(
                    width = 1.3.em,
                    height = 1.3.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = emojiKey,
                        modifier = Modifier,
                    )
                }
            }
        }.toMap()
}

/**
 * 清空分享图片缓存目录
 */
fun clearShareImageCache(context: Context) {
    java.io.File(context.externalCacheDir, "share_images").deleteRecursively()
}
