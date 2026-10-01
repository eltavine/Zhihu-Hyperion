/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Co-author: eltavine <me@eltavine.com>
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

package com.github.zly2006.zhihu.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIUserInterfaceStyle

/** Compose 跟踪界面的特征集合变化，系统切换深浅色时会重组。 */
@Composable
actual fun currentSystemInDarkTheme(): Boolean = isSystemInDarkTheme()

/**
 * 让状态栏、键盘和系统菜单跟随应用主题：样式设在承载界面的窗口上，SwiftUI 宿主控制器也一并生效。
 *
 * 跟随系统时清除覆盖，否则系统深浅色的变化传不进来，[currentSystemInDarkTheme] 会停在覆盖值上。
 */
@Composable
actual fun PlatformSystemBarEffect(darkTheme: Boolean) {
    val viewController = LocalUIViewController.current
    val followsSystem = ThemeManager.getThemeMode() == ThemeMode.SYSTEM
    LaunchedEffect(viewController, darkTheme, followsSystem) {
        val style = when {
            followsSystem -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            darkTheme -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            else -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        }
        viewController.view.window?.overrideUserInterfaceStyle = style
        viewController.overrideUserInterfaceStyle = style
    }
}
