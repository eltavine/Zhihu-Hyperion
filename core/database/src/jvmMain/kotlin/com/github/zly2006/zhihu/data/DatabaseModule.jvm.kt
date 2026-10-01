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

import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.local.getLocalContentDatabase
import org.koin.dsl.module
import org.koin.dsl.onClose
import java.io.File

/** 每个数据库在进程内只有一个 Room 实例；组合根传入各自的存量文件路径。 */
fun databaseModule(
    contentFilterDatabaseFile: File,
    localContentDatabaseFile: File,
) = module {
    single { getContentFilterDatabase(contentFilterDatabaseFile.also { it.parentFile?.mkdirs() }) } onClose { it?.close() }
    single { getLocalContentDatabase(localContentDatabaseFile.also { it.parentFile?.mkdirs() }) } onClose { it?.close() }
}
