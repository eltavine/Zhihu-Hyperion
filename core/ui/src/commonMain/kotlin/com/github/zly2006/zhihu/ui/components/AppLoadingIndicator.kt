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

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 页面、列表加载更多、弹窗内容等区块的不定时加载指示（Material 3 Expressive loading indicator，形状循环变形）。
 *
 * 只用于很快会结束的等待；按钮或行内的小号转圈，以及有明确进度的场景，继续使用 CircularProgressIndicator /
 * LinearProgressIndicator。Expressive 加载指示器仍是实验 API，只在这里使用，上游 API 变化时调用方不受影响。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoadingIndicator(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier)
}
