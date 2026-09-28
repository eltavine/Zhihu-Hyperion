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

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit

/** Application log facade over Kermit, keeping the `(tag, message, throwable)` order used throughout the code base. */
object Log {
    private val logger = Logger(loggerConfigInit(defaultLogWriter()), tag = "Zhihu++")

    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = logger.d(message, throwable, tag)

    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = logger.i(message, throwable, tag)

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = logger.w(message, throwable, tag)

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) = logger.e(message, throwable, tag)
}

internal expect fun defaultLogWriter(): LogWriter
