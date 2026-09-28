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

package com.github.zly2006.zhihu.util

/** 依次按响应头、文件扩展名与文件头字节推断图片 MIME 类型，都无法判断时按 JPEG 处理。 */
fun resolveArticleExportImageMimeType(
    contentTypeHeader: String?,
    imageUrl: String,
    imageBytes: ByteArray,
): String {
    contentTypeHeader
        ?.substringBefore(';')
        ?.trim()
        ?.takeIf { it.startsWith("image/") }
        ?.let { return it }

    guessArticleExportImageMimeTypeFromName(imageUrl)?.let { return it }
    guessArticleExportImageMimeTypeFromBytes(imageBytes)?.let { return it }
    return "image/jpeg"
}

private fun guessArticleExportImageMimeTypeFromName(imageUrl: String): String? =
    imageUrl
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .let { extension ->
            when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                "svg", "svgz" -> "image/svg+xml"
                "avif" -> "image/avif"
                "heic" -> "image/heic"
                "heif" -> "image/heif"
                else -> null
            }
        }

private fun guessArticleExportImageMimeTypeFromBytes(imageBytes: ByteArray): String? {
    fun matches(vararg values: Int): Boolean =
        imageBytes.size >= values.size &&
            values.indices.all { index -> imageBytes[index].toInt() and 0xff == values[index] }

    return when {
        matches(0xff, 0xd8, 0xff) -> "image/jpeg"

        matches(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a) -> "image/png"

        matches(0x47, 0x49, 0x46, 0x38) -> "image/gif"

        matches(0x42, 0x4d) -> "image/bmp"

        imageBytes.size >= 12 &&
            matches(0x52, 0x49, 0x46, 0x46) &&
            imageBytes[8].toInt().toChar() == 'W' &&
            imageBytes[9].toInt().toChar() == 'E' &&
            imageBytes[10].toInt().toChar() == 'B' &&
            imageBytes[11].toInt().toChar() == 'P' -> "image/webp"

        else -> null
    }
}
