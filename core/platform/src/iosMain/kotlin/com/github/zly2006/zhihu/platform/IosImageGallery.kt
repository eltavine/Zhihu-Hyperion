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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.github.zly2006.zhihu.ui.components.OpenImagePreviewContent
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIModalTransitionStyleCrossDissolve
import platform.UIKit.UIViewController

/**
 * iOS 的应用内看图：在当前界面上盖一个全屏的 Compose 页面，复用 [OpenImagePreviewContent]（左右翻页、长按菜单），
 * 图片由 telephoto 提供双指缩放和双击放大。看图页沿用打开它时的配色。
 */
@Composable
actual fun rememberImageGalleryOpener(): ImageGalleryOpener {
    val colorScheme = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    val typography = MaterialTheme.typography
    val openExternalUrl = rememberExternalUrlOpener()
    return remember(colorScheme, shapes, typography, openExternalUrl) {
        object : ImageGalleryOpener {
            override fun invoke(urls: List<String>, initialIndex: Int) {
                if (urls.isEmpty()) return
                var viewer: UIViewController? = null
                viewer = ComposeUIViewController {
                    MaterialTheme(colorScheme = colorScheme, shapes = shapes, typography = typography) {
                        val snackbarHostState = remember { SnackbarHostState() }
                        val scope = rememberCoroutineScope()
                        val messages = remember(snackbarHostState, scope) {
                            object : UserMessageSink {
                                override fun showShortMessage(message: String) {
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }

                                override fun showLongMessage(message: String) {
                                    scope.launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long) }
                                }
                            }
                        }
                        CompositionLocalProvider(LocalNativeUserMessageSink provides messages) {
                            Box(Modifier.fillMaxSize()) {
                                OpenImagePreviewContent(
                                    urls = urls,
                                    initialIndex = initialIndex,
                                    onDismiss = { viewer?.dismissViewControllerAnimated(true, completion = null) },
                                    onOpenInBrowser = { openExternalUrl(it) },
                                ) { url, onClick, onLongClick, onPageSwipeEnabledChange ->
                                    val zoomableState = rememberZoomableState()
                                    val painter = rememberAsyncImagePainter(url)
                                    val painterState by painter.state.collectAsState()
                                    LaunchedEffect(painterState) {
                                        if (painterState is AsyncImagePainter.State.Success) {
                                            zoomableState.setContentLocation(
                                                ZoomableContentLocation.scaledInsideAndCenterAligned(painter.intrinsicSize),
                                            )
                                        }
                                    }
                                    // 放大后左右拖动是在移动图片，暂停翻页。
                                    LaunchedEffect(zoomableState) {
                                        snapshotFlow { zoomableState.zoomFraction }.collect { zoomFraction ->
                                            onPageSwipeEnabledChange((zoomFraction ?: 0f) <= 0.01f)
                                        }
                                    }
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painter,
                                            contentDescription = null,
                                            contentScale = ContentScale.Inside,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .zoomable(zoomableState, onClick = { onClick() }, onLongClick = onLongClick),
                                        )
                                        if (painterState is AsyncImagePainter.State.Loading) {
                                            CircularProgressIndicator(color = Color.White)
                                        }
                                    }
                                }
                                SnackbarHost(
                                    snackbarHostState,
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .safeDrawingPadding()
                                        .padding(16.dp),
                                )
                            }
                        }
                    }
                }.apply {
                    modalPresentationStyle = UIModalPresentationOverFullScreen
                    modalTransitionStyle = UIModalTransitionStyleCrossDissolve
                    view.backgroundColor = UIColor.blackColor
                }
                presentFromTopViewController(checkNotNull(viewer))
            }
        }
    }
}

@Composable
actual fun rememberImagePreviewOpener(): ImagePreviewOpener {
    val openGallery = rememberImageGalleryOpener()
    return remember(openGallery) {
        object : ImagePreviewOpener {
            override fun invoke(url: String) = openGallery(listOf(url), 0)
        }
    }
}
