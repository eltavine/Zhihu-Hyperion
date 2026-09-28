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

import android.content.Context
import com.github.zly2006.zhihu.account.accountModule
import com.github.zly2006.zhihu.data.dataModule
import com.github.zly2006.zhihu.data.databaseModule
import com.github.zly2006.zhihu.notification.AndroidNotificationSettingsStore
import com.github.zly2006.zhihu.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.platform.androidUserMessageSink
import com.github.zly2006.zhihu.ui.AndroidArticleNavigationHandoff
import com.github.zly2006.zhihu.viewmodel.AndroidMobileClientProvider
import com.github.zly2006.zhihu.viewmodel.MobileClientProvider
import com.github.zly2006.zhihu.viewmodel.filter.HomeFeedFilter
import com.github.zly2006.zhihu.viewmodel.filter.KeywordSemanticMatcher
import kotlinx.io.files.Path
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

/** Android 进程的全部 Koin 绑定，由 app 的组合根启动。 */
fun androidZhihuModules(context: Context): List<Module> = listOf(
    accountModule(Path(File(context.filesDir, "account.json").path)),
    databaseModule(context),
    dataModule(Path(File(context.filesDir, "history.json").path)),
    zhihuSharedModule,
    module {
        single { AndroidArticleNavigationHandoff() }
        single { androidSettingsStore(context) }
        single<NotificationSettingsStore> { AndroidNotificationSettingsStore(context.applicationContext) }
        single<MobileClientProvider> { AndroidMobileClientProvider(get(), get(), get()) }
        single {
            HomeFeedFilter(get(), get(), get(), semanticMatcher = get<KeywordSemanticMatcher>()) { blocked ->
                androidUserMessageSink(context).showShortMessage("NLP 已屏蔽 ${blocked.first().title.take(10)}... 等 ${blocked.size} 条内容")
            }
        }
    },
)
