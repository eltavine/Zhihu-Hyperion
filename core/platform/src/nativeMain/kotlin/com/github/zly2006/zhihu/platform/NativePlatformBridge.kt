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

import platform.Foundation.NSBundle

expect val nativeIsDesktop: Boolean

val nativeAppVersionName: String
    get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "0.0.0"

expect fun copyNativePlainText(text: String)

expect fun nativeAccountFilePath(): String

expect fun nativeAppPrivateDirectoryPath(): String

internal expect fun nativeDownloadsDirectoryPath(): String

expect fun nativeChooseBlocklistImportFilePath(): String?

fun nativeBundledResourcePath(relativePath: String): String? =
    NSBundle.mainBundle.resourcePath?.let { resourceDirectory -> "$resourceDirectory/$relativePath" }

expect fun nativeSettingsStore(relativePath: String): SettingsStore
