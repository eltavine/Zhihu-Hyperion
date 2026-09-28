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

package com.github.zly2006.zhihu

import com.github.zly2006.zhihu.account.accountModule
import com.github.zly2006.zhihu.data.databaseModule
import com.github.zly2006.zhihu.desktop.desktopZhihuDataFile
import com.github.zly2006.zhihu.desktop.desktopZhihuLegacyAccountFile
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import kotlinx.io.files.Path
import org.koin.core.module.Module

/** 桌面进程的全部 Koin 绑定，由 desktopApp 的组合根启动。 */
fun desktopZhihuModules(): List<Module> = listOf(
    accountModule(Path(desktopZhihuLegacyAccountFile().toString())),
    databaseModule(desktopContentFilterDatabaseFile(), desktopZhihuDataFile("local-content.db")),
    zhihuSharedModule,
)
