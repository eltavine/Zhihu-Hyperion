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

package com.github.zly2006.zhihu.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * 知乎用户性别，编码与知乎 Web 前端的映射表一致：`1` 男、`0` 女、`-1` 未知（匿名用户也是 `-1`）。
 *
 * `0` 表示“女”，所以字段缺失、为 null 或出现未定义的编码时都解码为 [Unknown]，不能落到 `0`。
 */
@Serializable(with = GenderSerializer::class)
enum class Gender(
    val code: Int,
) {
    Male(1),
    Female(0),
    Unknown(-1),
}

private object GenderSerializer : KSerializer<Gender> {
    override val descriptor = PrimitiveSerialDescriptor("com.github.zly2006.zhihu.data.Gender", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Gender) = encoder.encodeInt(value.code)

    override fun deserialize(decoder: Decoder): Gender {
        val code = if (decoder is JsonDecoder) {
            (decoder.decodeJsonElement() as? JsonPrimitive)?.intOrNull
        } else {
            decoder.decodeInt()
        }
        return Gender.entries.firstOrNull { it.code == code } ?: Gender.Unknown
    }
}
