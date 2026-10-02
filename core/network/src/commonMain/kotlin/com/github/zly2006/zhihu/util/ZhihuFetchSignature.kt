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

@file:Suppress("ktlint:standard:argument-list-wrapping")

package com.github.zly2006.zhihu.util

import com.github.zly2006.zhihu.data.ZhihuJson
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.serializer
import org.kotlincrypto.hash.md.MD5

const val ZHIHU_WEB_ZSE93 = "101_3_3.0"

fun HttpRequestBuilder.signZhihuFetchRequest(
    zse93: String = ZHIHU_WEB_ZSE93,
    dc0: String,
    body: String? = null,
) {
    val requestUrl = url.buildString()
    header("x-zse-93", zse93)
    header("x-zse-96", ZhihuFetchSignature.createZse96Header(zse93, requestUrl, dc0, body))
    header("x-requested-with", "fetch")
}

fun HttpRequestBuilder.signZhihuFetchRequest(
    cookies: Map<String, String>,
    body: String? = null,
) {
    val dc0 = cookies["d_c0"]?.takeIf { it.isNotBlank() } ?: return
    val requestBody = body ?: if (contentType() == ContentType.Application.Json) {
        this.body as? String
            ?: bodyType?.kotlinType?.let { type ->
                ZhihuJson.json.encodeToString(serializer(type), this.body)
            }
    } else {
        null
    }
    signZhihuFetchRequest(dc0 = dc0, body = requestBody)
}

object ZhihuFetchSignature {
    fun createZse96Header(
        zse93: String,
        url: String,
        dc0: String,
        body: String? = null,
    ): String {
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = listOfNotNull(
            zse93,
            pathname,
            dc0,
            body,
        ).joinToString("+")
        return "2.0_${ZseSigner.encryptZseV4(md5Hex(signSource))}"
    }

    fun md5Hex(input: String): String = md5Hex(input.encodeToByteArray())

    fun md5Hex(message: ByteArray): String = MD5().digest(message).toHexString()
}
