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
import com.github.zly2006.zhihu.data.dataModule
import com.github.zly2006.zhihu.data.databaseModule
import com.github.zly2006.zhihu.notification.nativeNotificationSettingsStore
import com.github.zly2006.zhihu.platform.nativeAccountFilePath
import com.github.zly2006.zhihu.platform.nativeAppPrivateDirectoryPath
import com.github.zly2006.zhihu.platform.nativeSettingsStore
import com.github.zly2006.zhihu.util.AtomicTextFile
import com.github.zly2006.zhihu.viewmodel.AccountWebClientProvider
import com.github.zly2006.zhihu.viewmodel.MobileClientProvider
import com.github.zly2006.zhihu.viewmodel.WebDavConfigFile
import com.github.zly2006.zhihu.viewmodel.filter.HomeFeedFilter
import kotlinx.io.files.Path
import org.koin.core.module.Module
import org.koin.dsl.module

/** 原生进程的全部 Koin 绑定；账户路径在启动时解析，调用前必须先配置好调试数据目录等进程环境。 */
fun nativeZhihuModules(): List<Module> = listOf(
    accountModule(Path(nativeAccountFilePath())),
    databaseModule(nativeAppPrivateDirectoryPath()),
    dataModule(Path("${nativeAppPrivateDirectoryPath()}/history.json")),
    zhihuSharedModule,
    module {
        single { nativeSettingsStore("settings.properties") }
        single { WebDavConfigFile(AtomicTextFile(Path("${nativeAppPrivateDirectoryPath()}/webdav.json"))) }
        single { nativeNotificationSettingsStore() }
        single { HomeFeedFilter(get(), get(), get()) }
        single<MobileClientProvider> { AccountWebClientProvider(get()) }
    },
)
