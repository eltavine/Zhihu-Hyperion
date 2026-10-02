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
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.util.suspendRunCatching
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.compose.koinInject
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock

/** 从当前最上层的界面弹出 [controller]，看图页里再打开分享面板时也盖在看图页之上。 */
@OptIn(ExperimentalForeignApi::class)
internal fun presentFromTopViewController(controller: UIViewController) {
    val window = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?.keyWindow
    var presenter = window?.rootViewController ?: return
    while (true) presenter = presenter.presentedViewController ?: break
    // iPad 上分享面板以 popover 弹出，没有锚点会直接崩溃；锚在页面中央、不画箭头。
    controller.popoverPresentationController?.let { popover ->
        popover.sourceView = presenter.view
        presenter.view.bounds.useContents {
            popover.sourceRect = CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
        }
        popover.permittedArrowDirections = 0uL
    }
    presenter.presentViewController(controller, animated = true, completion = null)
}

/** 用系统分享面板分享 [items]：文字、链接或本地文件地址。 */
fun presentShareSheet(items: List<Any>) {
    presentFromTopViewController(UIActivityViewController(activityItems = items, applicationActivities = null))
}

internal actual suspend fun storeNativeImage(bytes: ByteArray, extension: String): String {
    val status = suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { continuation.resume(it) }
    }
    check(status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) {
        "没有添加到“照片”的权限，可以在系统设置里为 Zhihu-Hyperion 打开"
    }
    // 按原始数据写入，GIF 存进“照片”后仍是动图；UIImage 会把它压成一帧 JPEG。
    val data = bytes.toNSData()
    suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            changeBlock = {
                PHAssetCreationRequest.creationRequestForAsset().addResourceWithType(PHAssetResourceTypePhoto, data, options = null)
            },
            completionHandler = { success, error ->
                if (success) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(IllegalStateException(error?.localizedDescription ?: "写入“照片”失败"))
                }
            },
        )
    }
    return "已存入“照片”"
}

@Composable
actual fun rememberImageSharer(): ImageSharer {
    val scope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val accountStore = koinInject<ZhihuAccountStore>()
    return remember(scope, userMessages, accountStore) {
        object : ImageSharer {
            override fun invoke(url: String) {
                scope.launch {
                    suspendRunCatching {
                        val image = downloadNativeImage(accountStore, url)
                        // 分享图片文件而不是链接：收到的一方直接得到图片，GIF 也保持原格式。
                        val path = "${NSTemporaryDirectory()}zhihu-image-${Clock.System.now().toEpochMilliseconds()}.${image.extension}"
                        check(NSFileManager.defaultManager.createFileAtPath(path, contents = image.bytes.toNSData(), attributes = null)) {
                            "无法写入临时文件"
                        }
                        presentShareSheet(listOf(NSURL.fileURLWithPath(path)))
                    }.onFailure { error ->
                        userMessages.showShortMessage("分享失败: ${error.message}")
                    }
                }
            }
        }
    }
}
