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

package com.github.zly2006.zhihu.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * 应用的 Material 3 Expressive 主题：弹簧物理的 expressive 动效、Expressive 形状尺度与强调字体样式都来自
 * [MaterialExpressiveTheme] 的默认值。平台提供壁纸取色时用它，否则（关闭动态取色、桌面、macOS、Android 12 以下）
 * 用主题色按 2025 版配色规范生成配色，与 Android 16 的 Expressive 配色一致。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZhihuTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = ThemeManager.isDarkTheme()
    val seedColor = ThemeManager.getCustomColor()
    val platformColorScheme = if (ThemeManager.getUseDynamicColor()) platformDynamicColorScheme(darkTheme) else null
    val seededColorScheme = remember(seedColor, darkTheme) {
        dynamicColorScheme(seedColor = seedColor, isDark = darkTheme, specVersion = ColorSpec.SpecVersion.SPEC_2025)
    }
    val customBackgroundColor = ThemeManager.getBackgroundColor()
    val colorScheme = (platformColorScheme ?: seededColorScheme).copy(
        background = customBackgroundColor,
        surface = customBackgroundColor,
    )

    PlatformSystemBarEffect(darkTheme)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content,
    )
}

@Composable
expect fun currentSystemInDarkTheme(): Boolean

@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
expect fun PlatformSystemBarEffect(darkTheme: Boolean)
