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

import android.content.Context
import androidx.room.Room
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.buildContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.local.LocalContentDatabase
import com.github.zly2006.zhihu.viewmodel.local.buildLocalContentDatabase
import org.koin.dsl.module
import org.koin.dsl.onClose

/** 每个数据库在进程内只有一个 Room 实例；数据库名是已安装用户的存量文件名，不能修改。 */
fun databaseModule(context: Context) = module {
    single {
        buildContentFilterDatabase(
            Room.databaseBuilder<ContentFilterDatabase>(context.applicationContext, "content_filter_database"),
        )
    } onClose { it?.close() }
    single {
        buildLocalContentDatabase(
            Room.databaseBuilder<LocalContentDatabase>(context.applicationContext, "local_content_database"),
        )
    } onClose { it?.close() }
}
