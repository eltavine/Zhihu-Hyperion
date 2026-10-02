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

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun formatCompactCount(count: Int): String = when {
    count >= 10_000 -> {
        if (count % 10_000 == 0) {
            "${count / 10_000} 万"
        } else {
            val roundedTenths = ((count / 10_000.0) * 10).roundToInt() / 10.0
            "$roundedTenths 万"
        }
    }

    else -> {
        count.toString()
    }
}

fun formatDailyDate(dateString: String): String {
    if (dateString.length != 8 || dateString.any { !it.isDigit() }) {
        return dateString
    }
    return "${dateString.substring(0, 4)}年${dateString.substring(4, 6)}月${dateString.substring(6, 8)}日"
}

@OptIn(ExperimentalTime::class)
fun formatRelativeTime(
    epochSeconds: Long,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val diff = nowEpochSeconds - epochSeconds

    return when {
        diff < 60 -> {
            "刚刚"
        }

        diff < 3_600 -> {
            "${diff / 60}分钟前"
        }

        diff < 86_400 -> {
            "${diff / 3_600}小时前"
        }

        diff < 604_800 -> {
            "${diff / 86_400}天前"
        }

        else -> {
            val dateTime = Instant
                .fromEpochSeconds(epochSeconds)
                .toLocalDateTime(timeZone)
            "${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()} ${dateTime.hour.twoDigitString()}:${dateTime.minute.twoDigitString()}"
        }
    }
}

fun Int.twoDigitString(): String = toString().padStart(2, '0')
