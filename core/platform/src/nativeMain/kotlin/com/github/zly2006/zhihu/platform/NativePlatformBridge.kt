/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2026, eltavine <me@eltavine.com>
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

expect fun nativeChooseBlocklistImportFilePath(): String?

fun nativeBundledResourcePath(relativePath: String): String? =
    NSBundle.mainBundle.resourcePath?.let { resourceDirectory -> "$resourceDirectory/$relativePath" }

fun nativeSettingsStore(relativePath: String): SettingsStore =
    propertiesFileSettingsStore("${nativeAppPrivateDirectoryPath()}/$relativePath")

actual val isAigcVoteSupported: Boolean = true

actual val isFeedQualityFilterSupported: Boolean = true

actual val isLegacyWebViewSupported: Boolean = false
