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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcon
import com.github.zly2006.zhihu.icons.Icon

/** [ShapedIcon] 的容器形状，取自 Material 3 Expressive 的形状库；页面只能从这几种里选。 */
enum class IconShape {
    Cookie,
    Sunny,
    Clover,
    Flower,
}

/**
 * Expressive 异形容器里的图标，给空状态、引导页和页面头部这类需要视觉重心的位置用。
 *
 * 图标边长是容器的一半。默认用 secondaryContainer；页面的主视觉用 primaryContainer。
 */
@Composable
fun ShapedIcon(
    icon: AppIcon,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    shape: IconShape = IconShape.Cookie,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    val polygon = when (shape) {
        IconShape.Cookie -> MaterialShapes.Cookie9Sided
        IconShape.Sunny -> MaterialShapes.Sunny
        IconShape.Clover -> MaterialShapes.Clover4Leaf
        IconShape.Flower -> MaterialShapes.Flower
    }
    Box(
        modifier
            .size(size)
            .background(containerColor, polygon.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(size / 2),
            tint = contentColorFor(containerColor),
        )
    }
}
