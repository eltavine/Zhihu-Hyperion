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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.github.zly2006.zhihu.icons.AppIcon
import com.github.zly2006.zhihu.icons.Icon

/** [ChoiceButtonGroup] 中的一个选项；[testTag] 供 UI 自动化定位这个按钮。 */
@Immutable
class ChoiceOption<out T>(
    val value: T,
    val label: String,
    val icon: AppIcon? = null,
    val testTag: String? = null,
)

/**
 * 单选设置的连接式按钮组（Material 3 Expressive connected button group）。
 *
 * 两到三个短标签的互斥选项用它代替下拉框或一排普通按钮：所有选项一眼可见，选中项填充主色并变得更圆。
 * 选项多、标签长或选项随数据变化时继续使用下拉菜单。Expressive 按钮组仍是实验 API，只在这里使用，调用方不依赖它。
 */
@Composable
fun <T> ChoiceButtonGroup(
    options: List<ChoiceOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val checked = option.value == selected
            ToggleButton(
                // ToggleButton 只有一组禁用色，禁用时选中项会和其他项一样；让选中项深一档，仍能看出当前值。
                colors = if (checked) {
                    ToggleButtonDefaults.toggleButtonColors(
                        disabledContainerColor = colorScheme.onSurface.copy(alpha = 0.32f),
                        disabledContentColor = colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                } else {
                    ToggleButtonDefaults.toggleButtonColors()
                },
                checked = checked,
                onCheckedChange = { if (!checked) onSelect(option.value) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton }
                    .then(option.testTag?.let { Modifier.testTag(it) } ?: Modifier),
                enabled = enabled,
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                option.icon?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                }
                Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
