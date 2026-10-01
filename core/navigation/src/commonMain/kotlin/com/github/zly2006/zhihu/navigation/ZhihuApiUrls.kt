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

package com.github.zly2006.zhihu.navigation

fun zhihuQuestionFeedsUrl(
    questionId: Long,
    limit: Int,
    order: String? = null,
): String = zhihuQuestionFeedsUrl(questionId.toString(), limit, order)

fun zhihuQuestionFeedsUrl(
    questionId: String,
    limit: Int,
    order: String? = null,
): String =
    buildString {
        append("https://www.zhihu.com/api/v4/questions/")
        append(questionId)
        append("/feeds?limit=")
        append(limit)
        if (order != null) {
            append("&order=")
            append(order)
        }
    }
