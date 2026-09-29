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

package com.github.zly2006.zhihu.platform

import android.content.Context
import androidx.core.content.edit

/** 已安装用户的偏好文件名，改动会丢失全部设置。 */
const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"

fun androidSettingsStore(context: Context): SettingsStore {
    val preferences = context.applicationContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    return object : SettingsStore {
        override fun getBoolean(key: String, defaultValue: Boolean) = preferences.getBoolean(key, defaultValue)

        override fun putBoolean(key: String, value: Boolean) = preferences.edit { putBoolean(key, value) }

        override fun getString(key: String, defaultValue: String) = preferences.getString(key, defaultValue) ?: defaultValue

        override fun putString(key: String, value: String) = preferences.edit { putString(key, value) }

        override fun getStringOrNull(key: String) = preferences.getString(key, null)

        override fun putStringSet(key: String, value: Set<String>) = preferences.edit { putStringSet(key, value) }

        override fun getStringSet(key: String, defaultValue: Set<String>) = preferences.getStringSet(key, defaultValue)?.toSet() ?: defaultValue

        override fun getInt(key: String, defaultValue: Int) = preferences.getInt(key, defaultValue)

        override fun putInt(key: String, value: Int) = preferences.edit { putInt(key, value) }

        override fun getLong(key: String, defaultValue: Long) = preferences.getLong(key, defaultValue)

        override fun putLong(key: String, value: Long) = preferences.edit { putLong(key, value) }

        override fun getFloat(key: String, defaultValue: Float) = preferences.getFloat(key, defaultValue)

        override fun putFloat(key: String, value: Float) = preferences.edit { putFloat(key, value) }

        override fun remove(key: String) = preferences.edit { remove(key) }

        override fun contains(key: String) = preferences.contains(key)

        override fun removeByPrefix(prefix: String) = preferences.edit {
            preferences.all.keys
                .filter { it.startsWith(prefix) }
                .forEach(::remove)
        }

        override fun observeKeyChanges(onChanged: (String) -> Unit): AutoCloseable {
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key != null) {
                    onChanged(key)
                }
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            return AutoCloseable {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }
}
