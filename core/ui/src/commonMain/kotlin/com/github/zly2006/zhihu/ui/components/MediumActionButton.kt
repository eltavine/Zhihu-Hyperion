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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import com.github.zly2006.zhihu.icons.AppIcon
import com.github.zly2006.zhihu.icons.Icon

/** [MediumActionButton] 的强调程度，同一组按钮里只能有一个 [Filled]。 */
enum class ActionEmphasis {
    /** 页面的主操作。 */
    Filled,

    /** 与主操作并列、同样常用的操作。 */
    Tonal,

    /** 离开当前流程的操作，例如打开外部网页。 */
    Outlined,
}

/**
 * 页面级操作按钮：Material 3 Expressive 的中号尺寸（56dp），按下时形状收紧。
 *
 * [loading] 期间按钮不可点，文字原位隐藏（宽度和读屏文字都不变），上面叠加加载指示器。
 */
@Composable
fun MediumActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: ActionEmphasis = ActionEmphasis.Filled,
    icon: AppIcon? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val height: Dp = ButtonDefaults.MediumContainerHeight
    val shapes = ButtonDefaults.shapesFor(height)
    val contentPadding = ButtonDefaults.contentPaddingFor(height)
    val iconSize = ButtonDefaults.iconSizeFor(height)
    val buttonModifier = modifier.heightIn(min = height)
    val content: @Composable RowScope.() -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Row(Modifier.alpha(if (loading) 0f else 1f), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
                    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                }
                Text(text, style = ButtonDefaults.textStyleFor(height))
            }
            if (loading) {
                LoadingIndicator(Modifier.size(iconSize * 1.5f), color = LocalContentColor.current)
            }
        }
    }
    when (emphasis) {
        ActionEmphasis.Filled -> Button(
            onClick = onClick,
            shapes = shapes,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            contentPadding = contentPadding,
            content = content,
        )

        ActionEmphasis.Tonal -> FilledTonalButton(
            onClick = onClick,
            shapes = shapes,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            contentPadding = contentPadding,
            content = content,
        )

        ActionEmphasis.Outlined -> OutlinedButton(
            onClick = onClick,
            shapes = shapes,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            contentPadding = contentPadding,
            content = content,
        )
    }
}
