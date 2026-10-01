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

package com.github.zly2006.zhihu.platform

import java.io.File
import java.util.Properties

/** 以 java.util.Properties 格式读写的设置文件；创建时加载，写入时整文件回写，因此每个文件只能有一个实例。 */
class DesktopPropertiesFile(
    private val file: File,
    private val comments: String,
) {
    val properties: Properties = Properties()

    init {
        if (file.isFile) {
            file.inputStream().use(properties::load)
        }
    }

    fun save() {
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            properties.store(output, comments)
        }
    }
}

fun desktopSettingsStore(file: File): SettingsStore {
    val propertiesFile = DesktopPropertiesFile(file, "Zhihu-Hyperion desktop settings")
    val properties = propertiesFile.properties

    return object : SettingsStore {
        override fun getBoolean(key: String, defaultValue: Boolean) = properties.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue

        override fun putBoolean(key: String, value: Boolean) = write(key, value.toString())

        override fun getString(key: String, defaultValue: String) = properties.getProperty(key) ?: defaultValue

        override fun putString(key: String, value: String) = write(key, value)

        override fun getStringOrNull(key: String) = properties.getProperty(key)

        override fun putStringSet(key: String, value: Set<String>) = write(key, value.joinToString("\u001F"))

        override fun getStringSet(key: String, defaultValue: Set<String>) = properties
            .getProperty(key)
            ?.split("\u001F")
            ?.filter(String::isNotEmpty)
            ?.toSet() ?: defaultValue

        override fun getInt(key: String, defaultValue: Int) = properties.getProperty(key)?.toIntOrNull() ?: defaultValue

        override fun putInt(key: String, value: Int) = write(key, value.toString())

        override fun getLong(key: String, defaultValue: Long) = properties.getProperty(key)?.toLongOrNull() ?: defaultValue

        override fun putLong(key: String, value: Long) = write(key, value.toString())

        override fun getFloat(key: String, defaultValue: Float) = properties.getProperty(key)?.toFloatOrNull() ?: defaultValue

        override fun putFloat(key: String, value: Float) = write(key, value.toString())

        override fun remove(key: String) {
            properties.remove(key)
            propertiesFile.save()
        }

        override fun contains(key: String) = properties.containsKey(key)

        private fun write(key: String, value: String) {
            properties.setProperty(key, value)
            propertiesFile.save()
        }
    }
}
