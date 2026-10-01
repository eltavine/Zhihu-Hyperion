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

package com.github.zly2006.zhihu.platform

/** 键值偏好存储；每个平台在进程内只持有一个实例，由组合根绑定。 */
interface SettingsStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)

    fun getString(key: String, defaultValue: String): String

    fun putString(key: String, value: String)

    fun getStringOrNull(key: String): String?

    fun putStringSet(key: String, value: Set<String>)

    fun getStringSet(key: String, defaultValue: Set<String>): Set<String>

    fun getInt(key: String, defaultValue: Int): Int

    fun putInt(key: String, value: Int)

    fun getLong(key: String, defaultValue: Long): Long

    fun putLong(key: String, value: Long)

    fun getFloat(key: String, defaultValue: Float): Float

    fun putFloat(key: String, value: Float)

    fun remove(key: String)

    fun contains(key: String): Boolean

    fun observeKeyChanges(onChanged: (String) -> Unit): AutoCloseable = AutoCloseable { }
}
