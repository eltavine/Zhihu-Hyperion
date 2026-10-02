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
data class DailyStoriesResponse(
    val date: String,
    val stories: List<DailyStory>,
    /** 当天的头条大图，只有 `latest` 返回，按日期查询的 `before/<日期>` 没有这个字段。 */
    val topStories: List<DailyTopStory> = emptyList(),
)

@Serializable
data class DailyTopStory(
    val id: Long,
    val title: String,
    val url: String,
    val hint: String,
    val image: String,
)

@Serializable
data class DailyStory(
    val id: Long,
    val title: String,
    val url: String,
    val hint: String,
    val images: List<String>,
    val type: Int,
)

data class DailySection(
    val date: String,
    val stories: List<DailyStory>,
)
