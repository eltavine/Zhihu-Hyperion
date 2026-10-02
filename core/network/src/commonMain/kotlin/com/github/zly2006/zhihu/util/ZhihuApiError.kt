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

package com.github.zly2006.zhihu.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * 知乎没有返回数据，而是返回了 `{"error": {...}}` 错误体，例如未登录查看回答时的 HTTP 403。
 *
 * [message] 是知乎给用户看的原话，可以直接展示；[needLogin] 为 true 时登录后才能查看。
 */
class ZhihuApiErrorException(
    override val message: String,
    val needLogin: Boolean,
) : Exception(message)

/** 响应是知乎错误体时返回对应的异常，否则返回 null。 */
fun JsonObject.zhihuApiErrorOrNull(): ZhihuApiErrorException? {
    val error = this["error"] as? JsonObject ?: return null
    return ZhihuApiErrorException(
        message = (error["message"] as? JsonPrimitive)?.contentOrNull ?: "知乎暂时无法提供这项内容",
        needLogin = (error["need_login"] as? JsonPrimitive)?.booleanOrNull == true,
    )
}
