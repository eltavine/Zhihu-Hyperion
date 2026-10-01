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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.platform.SettingsStore

@Composable
fun <T> rememberObservedSetting(
    settings: SettingsStore,
    key: String,
    read: SettingsStore.() -> T,
): MutableState<T> {
    val state = remember(settings, key) { mutableStateOf(settings.read()) }
    DisposableEffect(settings, key, state) {
        val subscription = settings.observeKeyChanges { changedKey ->
            if (changedKey == key) state.value = settings.read()
        }
        onDispose(subscription::close)
    }
    return state
}
