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

package com.github.zly2006.zhihu.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement

object ZhihuJson {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Suppress("FunctionName")
    fun snakeCaseToCamelCase(snakeCase: String): String = snakeCase
        .split("_")
        .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
        .replaceFirstChar { it.lowercase() }

    fun snakeCaseToCamelCase(json: JsonElement): JsonElement = when (json) {
        is JsonObject -> buildJsonObject {
            for ((key, value) in json) {
                // cookie/cookies 的子键是服务端签发的动态凭据名，不是模型字段，必须逐字保留。
                put(
                    snakeCaseToCamelCase(key),
                    if (key == "cookie" || key == "cookies") value else snakeCaseToCamelCase(value),
                )
            }
        }

        is JsonArray -> buildJsonArray {
            for (item in json) {
                add(snakeCaseToCamelCase(item))
            }
        }

        else -> json
    }

    inline fun <reified T> decodeJson(json: JsonElement): T =
        this.json.decodeFromJsonElement(snakeCaseToCamelCase(json))

    fun <T> decodeJson(serializer: KSerializer<T>, json: JsonElement): T =
        this.json.decodeFromJsonElement(serializer, snakeCaseToCamelCase(json))
}
