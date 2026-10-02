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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.data.macosAppDataDirectoryPath
import com.github.zly2006.zhihu.data.macosBackgroundUiDebugDataDirectoryPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.Channel
import org.koin.mp.KoinPlatform
import platform.AppKit.NSModalResponseOK
import platform.AppKit.NSOpenPanel
import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardTypeString
import platform.AppKit.NSWorkspace
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import kotlin.time.Clock

actual val nativeIsDesktop: Boolean = true

fun isMacosQuitOnWindowCloseEnabled(): Boolean =
    KoinPlatform.getKoin().get<SettingsStore>().getBoolean(MACOS_QUIT_ON_WINDOW_CLOSE_PREFERENCE_KEY, false)

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberExternalUrlOpener(): ExternalUrlOpener = remember {
    object : ExternalUrlOpener {
        override fun invoke(url: String) {
            NSURL.URLWithString(url)?.let(NSWorkspace.sharedWorkspace::openURL)
        }
    }
}

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberWebViewUrlOpener(): WebViewUrlOpener = remember {
    object : WebViewUrlOpener {
        override fun invoke(url: String) {
            NSURL.URLWithString(url)?.let(NSWorkspace.sharedWorkspace::openURL)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun copyNativePlainText(text: String) {
    NSPasteboard.generalPasteboard.apply {
        clearContents()
        setString(text, forType = NSPasteboardTypeString)
    }
}

actual fun nativeAccountFilePath(): String =
    macosBackgroundUiDebugDataDirectoryPath()?.let { "$it/account.json" }
        ?: "${NSHomeDirectory()}/.zhihu-hyperion/account.json"

actual fun nativeAppPrivateDirectoryPath(): String =
    macosAppDataDirectoryPath()

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun storeNativeImage(bytes: ByteArray, extension: String): String {
    val downloadsDirectory = macosBackgroundUiDebugDataDirectoryPath()?.let { "$it/Downloads" }
        ?: "${NSHomeDirectory()}/Downloads"
    NSFileManager.defaultManager.createDirectoryAtPath(
        downloadsDirectory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    val filePath = "$downloadsDirectory/image_${Clock.System.now().toEpochMilliseconds()}.$extension"
    check(NSFileManager.defaultManager.createFileAtPath(filePath, contents = bytes.toNSData(), attributes = null)) {
        "无法写入 $filePath"
    }
    return "已保存图片: $filePath"
}

@Composable
actual fun rememberImagePreviewOpener(): ImagePreviewOpener = rememberExternalUrlOpener()

@Composable
actual fun rememberImageSharer(): ImageSharer {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        object : ImageSharer {
            override fun invoke(url: String) {
                runCatching {
                    copyNativePlainText(url)
                    userMessages.showShortMessage("已复制图片链接")
                }.onFailure { error ->
                    userMessages.showShortMessage("分享失败: ${error.message}")
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun nativeChooseBlocklistImportFilePath(): String? {
    val panel = NSOpenPanel.openPanel()
    panel.title = "导入屏蔽规则"
    panel.canChooseFiles = true
    panel.canChooseDirectories = false
    panel.allowsMultipleSelection = false
    return if (panel.runModal() == NSModalResponseOK) panel.URL?.path else null
}

actual val platformBottomBarItemLimit: Int? = null
