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

@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.zly2006.zhihu.account

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreImage.CIContext
import platform.CoreImage.CIFilter
import platform.CoreImage.createCGImage
import platform.CoreImage.filterWithName
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue
import platform.darwin.NSObject

/** macOS 与 iOS 共用：CoreImage 生成二维码，画进 CoreGraphics 位图后交给 Skia，不依赖 AppKit 或 UIKit。 */
actual fun generateQrLoginBitmap(content: String): ImageBitmap {
    val messageData = NSString.create(string = content).dataUsingEncoding(NSUTF8StringEncoding)
        ?: error("无法编码二维码内容")
    val filter = CIFilter.filterWithName("CIQRCodeGenerator") ?: error("系统不支持二维码生成器")
    (filter as NSObject).setValue(messageData, forKey = "inputMessage")
    (filter as NSObject).setValue("M", forKey = "inputCorrectionLevel")
    // 最近邻采样放大，模块边缘保持锐利，扫码更稳。
    val outputImage = filter.outputImage
        ?.imageBySamplingNearest()
        ?.imageByApplyingTransform(CGAffineTransformMakeScale(10.0, 10.0))
        ?: error("无法生成二维码图像")
    val cgImage = CIContext.contextWithOptions(null).createCGImage(outputImage, fromRect = outputImage.extent)
        ?: error("无法渲染二维码图像")
    try {
        val width = CGImageGetWidth(cgImage).toInt()
        val height = CGImageGetHeight(cgImage).toInt()
        val bytesPerRow = width * 4
        val pixels = ByteArray(bytesPerRow * height)
        pixels.usePinned { pinned ->
            val colorSpace = CGColorSpaceCreateDeviceRGB()
            val context = CGBitmapContextCreate(
                data = pinned.addressOf(0),
                width = width.toULong(),
                height = height.toULong(),
                bitsPerComponent = 8u,
                bytesPerRow = bytesPerRow.toULong(),
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
            )
            CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), cgImage)
            CGContextRelease(context)
            CGColorSpaceRelease(colorSpace)
        }
        return Image
            .makeRaster(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL), pixels, bytesPerRow)
            .toComposeImageBitmap()
    } finally {
        CGImageRelease(cgImage)
    }
}
