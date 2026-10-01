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

package com.github.zly2006.zhihu.ui.components

import android.content.Context
import android.graphics.Color.BLACK
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

class OpenImageDialog(
    context: Context,
    urls: List<String>,
    initialIndex: Int = 0,
) : ComponentDialog(context) {
    constructor(
        context: Context,
        url: String,
    ) : this(context, listOf(url), 0)

    private val imageUrls = urls
        .filter { it.isNotBlank() && !it.startsWith("data") }
        .distinct()
        .ifEmpty { listOf("") }
    private val initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex)

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
        setContentView(
            ComposeView(context).apply {
                setContent {
                    OpenImagePreviewContent(
                        urls = imageUrls,
                        initialIndex = initialPage,
                        onDismiss = { dismiss() },
                        onOpenInBrowser = { imageUrl ->
                            luoTianYiUrlLauncher(context, imageUrl.toUri())
                        },
                    ) { imageUrl, onClick, onLongClick, onPageSwipeEnabledChange ->
                        val imageState = rememberZoomableImageState(rememberZoomableState())
                        LaunchedEffect(imageState) {
                            snapshotFlow { imageState.zoomableState.zoomFraction }
                                .collect { zoomFraction ->
                                    onPageSwipeEnabledChange((zoomFraction ?: 0f) <= 0.01f)
                                }
                        }
                        ZoomableAsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            state = imageState,
                            onClick = { onClick() },
                            onLongClick = onLongClick,
                        )
                    }
                }
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        window?.setBackgroundDrawable(BLACK.toDrawable())
    }
}
