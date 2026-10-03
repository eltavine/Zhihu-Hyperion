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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPasteboard
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual val nativeIsDesktop: Boolean = false

// iPhone tab bars show at most five items, the same limit as Android phones.
actual val platformBottomBarItemLimit: Int? = 5

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberExternalUrlOpener(): ExternalUrlOpener = remember {
    object : ExternalUrlOpener {
        override fun invoke(url: String) {
            // iOS 18 起单参数的 openURL(url) 不再打开链接、只返回 false，而 Kotlin 绑定没有把它标成废弃。
            NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null) }
        }
    }
}

@Composable
actual fun rememberWebViewUrlOpener(): WebViewUrlOpener = remember {
    object : WebViewUrlOpener {
        override fun invoke(url: String) {
            NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null) }
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

actual suspend fun nativeChooseBlocklistImportFilePath(): String? =
    pickDocument(UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText), asCopy = true))?.path

actual suspend fun nativeDeliverExportedFile(path: String): String {
    val destination = pickDocument(UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(path)), asCopy = true))
    return if (destination != null) "已导出到“文件”" else "已取消导出"
}

/** 弹出系统文件选择器，返回用户选中的文件或保存位置；取消时为 null。 */
private suspend fun pickDocument(controller: UIDocumentPickerViewController): NSURL? = suspendCancellableCoroutine { continuation ->
    val picker = DocumentPicker(controller, continuation)
    // UIKit 只弱引用 delegate：取消回调持有它直到选择结束，协程被取消时顺带关掉选择器。
    continuation.invokeOnCancellation { picker.dismiss() }
    presentFromTopViewController(controller)
}

private class DocumentPicker(
    private val controller: UIDocumentPickerViewController,
    private val continuation: CancellableContinuation<NSURL?>,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    init {
        controller.delegate = this
    }

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) =
        continuation.resume(didPickDocumentsAtURLs.firstOrNull() as? NSURL)

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = continuation.resume(null)

    fun dismiss() = controller.dismissViewControllerAnimated(true, completion = null)
}
