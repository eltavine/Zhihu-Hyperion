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

/** 不支持持久化设置的平台使用的空实现：读取总是返回默认值，写入被忽略。 */
fun noopSettingsStore(): SettingsStore = object : SettingsStore {
    override fun getBoolean(key: String, defaultValue: Boolean) = defaultValue

    override fun putBoolean(key: String, value: Boolean) = Unit

    override fun getString(key: String, defaultValue: String) = defaultValue

    override fun putString(key: String, value: String) = Unit

    override fun getStringOrNull(key: String): String? = null

    override fun putStringSet(key: String, value: Set<String>) = Unit

    override fun getStringSet(key: String, defaultValue: Set<String>) = defaultValue

    override fun getInt(key: String, defaultValue: Int) = defaultValue

    override fun putInt(key: String, value: Int) = Unit

    override fun getLong(key: String, defaultValue: Long) = defaultValue

    override fun putLong(key: String, value: Long) = Unit

    override fun getFloat(key: String, defaultValue: Float) = defaultValue

    override fun putFloat(key: String, value: Float) = Unit

    override fun remove(key: String) = Unit

    override fun contains(key: String) = false
}
