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

package com.github.zly2006.zhihu.filter

import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase

/**
 * 内容打开事件的来源交接：导航前 [prepare] 暂存打开来源，内容页 [record] 时按内容身份取回一次并落库。
 * 页面级 environment 会反复重建，因此暂存状态由 Koin 持有的进程级单例承载。
 */
class ContentOpenTracker(
    private val database: ContentFilterDatabase,
) {
    private var identity: TrackedContentIdentity? = null
    private var openFrom: String? = null

    fun prepare(
        destination: NavDestination,
        openFrom: String,
    ) {
        identity = ContentOpenEventSupport.toTrackedContentIdentity(destination)
        this.openFrom = openFrom.takeIf { identity != null && it.isNotBlank() }
    }

    suspend fun record(
        destination: NavDestination,
        questionId: Long? = null,
    ) {
        ContentOpenEventSupport.recordOpenEvent(database, destination, questionId, consume(destination))
    }

    fun consume(destination: NavDestination): String {
        val destinationIdentity = ContentOpenEventSupport.toTrackedContentIdentity(destination) ?: return ContentOpenFrom.UNKNOWN
        if (destinationIdentity != identity) return ContentOpenFrom.UNKNOWN
        identity = null
        return openFrom.also { openFrom = null } ?: ContentOpenFrom.UNKNOWN
    }
}
