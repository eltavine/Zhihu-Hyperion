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

package com.github.zly2006.zhihu.data

import kotlinx.serialization.Serializable

@Serializable
data class FollowedQuestion(
    val id: String,
    val type: String = "question",
    val url: String = "",
    val title: String = "",
    val questionType: String = "",
    val created: Long = 0L,
    val updatedTime: Long = 0L,
)

@Serializable
data class FollowedTopic(
    val id: String = "",
    val type: String = "topic",
    val url: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val topicType: String? = null,
    val topic: DataHolder.Topic? = null,
) {
    val displayId: String get() = topic?.id ?: id
    val displayName: String get() = topic?.name ?: name
    val displayAvatarUrl: String? get() = topic?.avatarUrl ?: avatarUrl
}
