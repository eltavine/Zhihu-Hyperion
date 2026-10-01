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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

actual val nativeIsDesktop: Boolean = false

// iPhone tab bars show at most five items, the same limit as Android phones.
actual val platformBottomBarItemLimit: Int? = 5

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberExternalUrlOpener(): ExternalUrlOpener = remember {
    object : ExternalUrlOpener {
        override fun invoke(url: String) {
            NSURL.URLWithString(url)?.let(UIApplication.sharedApplication::openURL)
        }
    }
}

@Composable
actual fun rememberWebViewUrlOpener(): WebViewUrlOpener = remember {
    object : WebViewUrlOpener {
        override fun invoke(url: String) {
            NSURL.URLWithString(url)?.let(UIApplication.sharedApplication::openURL)
        }
    }
}

@Composable
actual fun rememberImageGalleryOpener(): ImageGalleryOpener {
    val openExternalUrl = rememberExternalUrlOpener()
    return remember(openExternalUrl) {
        object : ImageGalleryOpener {
            override fun invoke(urls: List<String>, initialIndex: Int) {
                if (urls.isNotEmpty()) {
                    openExternalUrl(urls[initialIndex.coerceIn(0, urls.lastIndex)])
                }
            }
        }
    }
}

actual fun copyNativePlainText(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

@OptIn(ExperimentalForeignApi::class)
actual fun nativeAccountFilePath(): String = "${nativeAppPrivateDirectoryPath()}/account.json"

/** 应用私有数据放在 Application Support：不出现在“文件”App 里，但会随设备备份。 */
@OptIn(ExperimentalForeignApi::class)
actual fun nativeAppPrivateDirectoryPath(): String = checkNotNull(
    NSFileManager.defaultManager
        .URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )?.path,
) { "iOS 没有提供 Application Support 目录" }

internal actual fun nativeDownloadsDirectoryPath(): String = "${nativeAppPrivateDirectoryPath()}/Downloads"

actual fun nativeChooseBlocklistImportFilePath(): String? = null
