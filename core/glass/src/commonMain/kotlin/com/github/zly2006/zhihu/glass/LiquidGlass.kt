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

package com.github.zly2006.zhihu.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.isRuntimeShaderSupported

/** 外观设置里的「液态玻璃」开关；为 true 时底部导航栏和阅读页操作栏改用液态玻璃。 */
const val LIQUID_GLASS_PREFERENCE_KEY = "liquidGlass"

/** 只有 iOS 默认开启，与系统的 Liquid Glass 视觉一致；其他平台由用户在外观设置里打开。 */
expect val isLiquidGlassEnabledByDefault: Boolean

/** iOS 的系统标签栏压在 Home 指示条的安全区里，其他平台的悬浮底栏留在系统导航栏之上。 */
internal expect val tabBarSitsInHomeIndicatorArea: Boolean

/** 透镜折射需要运行时着色器：Android 13 起支持，iOS、macOS 和桌面端的 Skia 始终支持。 */
val isLiquidGlassSupported: Boolean
    get() = isRuntimeShaderSupported()

/**
 * 玻璃表面采样的背景内容。内容宿主用 [glassBackdropSource] 录制，同一宿主里的玻璃表面再读取它。
 */
@Stable
class GlassBackdrop internal constructor(
    internal val layer: LayerBackdrop,
)

/**
 * 创建一份可供玻璃表面采样的背景。
 *
 * 录制时先铺满 [background]：内容透明的地方如果不铺底，玻璃下方未折射的原始画面会透出来，形成重影。
 */
@Composable
fun rememberGlassBackdrop(background: Color): GlassBackdrop {
    val layer = rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    }
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * 把这个节点绘制的内容录进 [backdrop]，[backdrop] 为 null 时不做任何事。
 *
 * 玻璃表面必须是这个节点的兄弟而不是子节点，否则会采样到自己。
 */
fun Modifier.glassBackdropSource(backdrop: GlassBackdrop?): Modifier = if (backdrop == null) this else layerBackdrop(backdrop.layer)

/** 胶囊形液态玻璃底板：提高背景饱和度、轻度模糊、在边缘折射背景，最后叠一层半透明表面色保证前景可读。 */
fun Modifier.liquidGlass(
    backdrop: GlassBackdrop,
    surfaceColor: Color,
): Modifier = drawBackdrop(
    backdrop = backdrop.layer,
    shape = { CircleShape },
    effects = {
        vibrancy()
        blur(8.dp.toPx())
        lens(24.dp.toPx(), 24.dp.toPx())
    },
    onDrawSurface = { drawRect(surfaceColor) },
)

object LiquidGlassDefaults {
    /** 叠在玻璃上的表面色，取自 MIUIX 液态玻璃导航栏的 40% 表面容器色。 */
    val surfaceColor: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
}
