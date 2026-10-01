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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcon
import com.github.zly2006.zhihu.icons.Icon

/** [EmptyState] 下方的操作按钮，例如“重试”；[testTag] 供 UI 自动化定位按钮。 */
@Immutable
class EmptyStateAction(
    val label: String,
    val icon: AppIcon,
    val testTag: String? = null,
    val onClick: () -> Unit,
)

/**
 * 空列表或加载失败时的占位：Expressive 异形容器里的图标、一句说明和可选的操作按钮。
 *
 * 用于整页空状态和列表底部的失败提示，让这些状态有统一的样子；只有一行的轻提示（如“已经到底啦”）不必用它。
 */
@Composable
fun EmptyState(
    icon: AppIcon,
    title: String,
    modifier: Modifier = Modifier,
    action: EmptyStateAction? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.let {
            FilledTonalButton(
                onClick = it.onClick,
                modifier = it.testTag?.let { tag -> Modifier.testTag(tag) } ?: Modifier,
            ) {
                Icon(it.icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(it.label)
            }
        }
    }
}
