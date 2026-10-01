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

package com.github.zly2006.zhihu.util

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

@Suppress("NOTHING_TO_INLINE")
inline fun ComponentActivity.enableEdgeToEdgeCompat() {
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 16 can add a dark protection scrim over transparent status bars
        // when contrast enforcement remains enabled, changing the app surface color.
        window.isStatusBarContrastEnforced = false
        // enableEdgeToEdge() keeps navigation bar contrast enforcement on for its default auto style
        // (androidx.activity 1.13 EdgeToEdgeApi29), so three-button navigation gets a system scrim.
        // Turning it off is the intended override for a fully transparent three-button bar:
        // https://issuetracker.google.com/issues/298296168
        window.isNavigationBarContrastEnforced = false
    }
}
