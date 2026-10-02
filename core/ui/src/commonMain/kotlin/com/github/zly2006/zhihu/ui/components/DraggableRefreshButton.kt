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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon
import com.github.zly2006.zhihu.platform.SettingsStore
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * 可拖动并自动贴边的刷新按钮。
 *
 * 这个 FAB 用于首页和其他列表页的手动刷新入口。位置按 [preferenceName] 分别保存到 `-x`、`-y` 两个 preference key，
 * 拖动结束后会限制在屏幕内并停靠到左右两侧，避免遮挡内容或被系统栏吞掉。需要多个可拖动按钮时必须使用不同的 [preferenceName]，
 * 并可用 [initiallyOnLeft] 把新按钮放到另一侧；用户已保存的位置仍始终优先。
 */
@Composable
fun DraggableRefreshButton(
    modifier: Modifier = Modifier,
    preferenceName: String = "fabRefresh",
    bottomAvoidance: Dp = 0.dp,
    initiallyOnLeft: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {
        Icon(AppIcons.Refresh, contentDescription = "刷新")
    },
) {
    val density = LocalDensity.current
    val screenSize = LocalWindowInfo.current.containerSize
    val settings = koinInject<SettingsStore>()

    var offsetX by remember(preferenceName, initiallyOnLeft) {
        mutableFloatStateOf(
            settings.getFloat(
                "$preferenceName-x",
                if (initiallyOnLeft) 0f else Float.MAX_VALUE,
            ),
        )
    }
    var offsetY by remember { mutableFloatStateOf(settings.getFloat("$preferenceName-y", Float.MAX_VALUE)) }
    var pressing by remember { mutableStateOf(false) }
    // 横向范围以按钮所在容器为准：宽屏左侧有导航轨时，容器比窗口窄，按窗口宽度贴右边会跑出屏幕。
    var containerWidthPx by remember { mutableIntStateOf(screenSize.width) }
    val maxStoredOffsetY = with(density) {
        (screenSize.height - 250.dp.toPx()).coerceAtLeast(0f)
    }
    val maxVisibleOffsetY = with(density) {
        (maxStoredOffsetY - bottomAvoidance.toPx()).coerceAtLeast(0f)
    }

    // 停靠时与边缘留出 Scaffold 给悬浮按钮的 16dp 间距，和同一侧的其它悬浮按钮对齐。
    val edgeMarginPx = with(density) { 16.dp.toPx() }

    fun adjustFabPosition() {
        with(density) {
            offsetX = offsetX.coerceIn(edgeMarginPx, (containerWidthPx - 56.dp.toPx() - edgeMarginPx).coerceAtLeast(edgeMarginPx))
            offsetY = offsetY.coerceIn(0f, maxStoredOffsetY)
        }
    }

    adjustFabPosition()
    val visibleOffsetY = offsetY.coerceAtMost(maxVisibleOffsetY)

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetX",
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = visibleOffsetY,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetY",
    )
    val displayedOffsetX = if (pressing) offsetX else animatedOffsetX
    val displayedOffsetY = if (pressing) visibleOffsetY else animatedOffsetY
    val hapticFeedback = LocalHapticFeedback.current

    val opacityFraction = remember(settings) {
        settings.getInt(PREF_FAB_OPACITY, DEFAULT_FAB_OPACITY).coerceIn(10, 100) / 100f
    }
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = FloatingActionButtonDefaults.containerColor.copy(alpha = opacityFraction),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = opacityFraction),
        elevation = if (opacityFraction < 1f) {
            FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
        } else {
            FloatingActionButtonDefaults.elevation()
        },
        modifier = modifier
            .onPlaced { coordinates ->
                coordinates.parentLayoutCoordinates
                    ?.size
                    ?.width
                    ?.let { containerWidthPx = it }
            }.offset { IntOffset(displayedOffsetX.roundToInt(), displayedOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        offsetX = animatedOffsetX
                        offsetY = animatedOffsetY
                        pressing = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        pressing = false
                        adjustFabPosition()
                        val containerWidth = containerWidthPx.toFloat()
                        with(density) {
                            offsetX =
                                if (offsetX < containerWidth / 2) {
                                    edgeMarginPx
                                } else {
                                    containerWidth - 56.dp.toPx() - edgeMarginPx
                                }
                        }
                        settings.putFloat("$preferenceName-x", offsetX)
                        settings.putFloat("$preferenceName-y", offsetY)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (pressing) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            adjustFabPosition()
                        }
                    },
                )
            },
        content = content,
    )
}
