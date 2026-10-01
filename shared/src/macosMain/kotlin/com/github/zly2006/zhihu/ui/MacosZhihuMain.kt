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

package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.MyCollections
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Search

/**
 * macOS Kotlin/Native 主界面入口。
 *
 * 页面和导航来自共享的 [NativeZhihuMain]；传入 [windowChrome] 时，一级导航、搜索和通知改由原生侧栏与工具栏承担。
 */
@Composable
fun MacosZhihuMain(windowChrome: MacosWindowChromeHost? = null) {
    NativeZhihuMain(
        chrome = windowChrome?.let { host ->
            { state, content -> host(macosWindowChrome(state), content) }
        },
    )
}

private fun macosWindowChrome(state: NativeMainChromeState) = MacosWindowChrome(
    navigationItems = state.mainTabs.mapNotNull { destination ->
        val selected = destination == state.currentMainTab
        val action = { state.openMainTab(destination) }
        when (destination) {
            Home -> MacosWindowNavigationItem(Home.name, "首页", "house", "内容", selected, action)
            Follow -> MacosWindowNavigationItem(Follow.name, "关注", "person.2", "内容", selected, action)
            HotList -> MacosWindowNavigationItem(HotList.name, "热榜", "flame", "内容", selected, action)
            Daily -> MacosWindowNavigationItem(Daily.name, "日报", "newspaper", "内容", selected, action)
            OnlineHistory -> MacosWindowNavigationItem(OnlineHistory.name, "历史", "clock.arrow.circlepath", "资料", selected, action)
            MyCollections -> MacosWindowNavigationItem(MyCollections.name, "收藏", "bookmark", "资料", selected, action)
            Account -> MacosWindowNavigationItem(Account.name, "账号", "person.crop.circle", "账号", selected, action)
            else -> null
        }
    },
    trailingToolbarItems = listOf(
        MacosWindowToolbarItem(
            identifier = "search",
            label = "搜索",
            systemSymbolName = "magnifyingglass",
            action = { state.navigate(Search()) },
        ),
        MacosWindowToolbarItem(
            identifier = "notifications",
            label = "通知",
            systemSymbolName = "bell",
            action = { state.navigate(Notification) },
        ),
    ),
)
