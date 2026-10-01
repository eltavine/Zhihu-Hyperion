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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.zly2006.zhihu.data

import androidx.room.Room
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.buildContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.local.LocalContentDatabase
import com.github.zly2006.zhihu.viewmodel.local.buildLocalContentDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose
import platform.Foundation.NSFileManager

/** macOS 与 iOS 共用的数据库绑定：两个 Room 数据库都用打包的 SQLite 驱动，文件放在 [dataDirectory] 下。 */
fun databaseModule(dataDirectory: String): Module = module {
    single {
        buildContentFilterDatabase(Room.databaseBuilder<ContentFilterDatabase>(name = databasePath(dataDirectory, "content-filter.db")))
    } onClose { it?.close() }
    single {
        buildLocalContentDatabase(Room.databaseBuilder<LocalContentDatabase>(name = databasePath(dataDirectory, "local-content.db")))
    } onClose { it?.close() }
}

private fun databasePath(
    dataDirectory: String,
    fileName: String,
): String {
    NSFileManager.defaultManager.createDirectoryAtPath(
        dataDirectory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return "$dataDirectory/$fileName"
}
