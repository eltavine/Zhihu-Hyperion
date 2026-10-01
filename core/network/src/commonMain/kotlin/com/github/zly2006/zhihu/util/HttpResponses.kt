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

package com.github.zly2006.zhihu.util

import com.github.zly2006.zhihu.data.ZhihuJson
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.jvm.JvmName

class HttpStatusException(
    val status: HttpStatusCode,
    val requestUrl: Url,
    val bodyText: String,
) : Exception() {
    override val message: String
        get() = "HTTP error: ${status.value} ${status.description} on $requestUrl: \n $bodyText"

    var dumpedCurlRequest: String? = null

    constructor(
        status: HttpStatusCode,
        requestUrl: Url,
        bodyText: String,
        dumpedCurlRequest: String?,
    ) : this(status, requestUrl, bodyText) {
        this.dumpedCurlRequest = dumpedCurlRequest
    }
}

suspend fun HttpResponse.raiseForStatus(dumpRequest: Boolean = false): HttpResponse {
    if (status.value >= 400) {
        throw HttpStatusException(
            status = status,
            requestUrl = request.url,
            bodyText = bodyAsText(),
            dumpedCurlRequest = if (dumpRequest) dumpCurlRequest() else null,
        )
    }
    return this
}

fun HttpResponse.dumpCurlRequest(): String {
    val sb = StringBuilder()
    sb.append("curl -X ${request.method.value} '${request.url}' ")
    request.headers.forEach { key, values ->
        values.forEach { value ->
            sb.append("\\\n  -H '$key: $value' ")
        }
    }
    return sb.toString()
}

suspend fun HttpResponse.json() = this.body<JsonElement>()

suspend fun HttpResponse.jsonObject() = this.body<JsonObject>()

@JvmName("jsonTyped")
suspend inline fun <reified T> HttpResponse.json() = ZhihuJson.decodeJson<T>(this.json())
