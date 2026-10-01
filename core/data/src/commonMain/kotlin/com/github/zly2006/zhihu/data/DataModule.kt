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

import com.github.zly2006.zhihu.filter.ContentOpenTracker
import com.github.zly2006.zhihu.filter.RemoteHistorySync
import com.github.zly2006.zhihu.util.AtomicTextFile
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.local.LocalContentDatabase
import com.github.zly2006.zhihu.viewmodel.local.buildLocalRecommendationEngine
import kotlinx.io.files.Path
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * :core:data 的进程级单例；[historyFile] 由各平台组合根给出，沿用既有 history.json 位置。
 * 本地推荐引擎依赖平台绑定的 LocalContentDatabase，没有该数据库的平台在请求引擎时直接失败。
 */
fun dataModule(historyFile: Path) = module {
    single { HistoryStorage(AtomicTextFile(historyFile)) }
    single { ContentOpenTracker(get()) }
    single { RemoteHistorySync(get(), get<ContentFilterDatabase>().contentOpenEventDao()) }
    single { buildLocalRecommendationEngine(get<LocalContentDatabase>().contentDao(), get()) } onClose { it?.close() }
}
