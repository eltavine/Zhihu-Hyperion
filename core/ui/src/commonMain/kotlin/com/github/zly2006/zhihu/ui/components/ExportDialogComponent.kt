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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcon
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private class ExportOption(
    val icon: AppIcon,
    val title: String,
    val description: String?,
    val onClick: () -> Unit,
)

/**
 * 导出文章的对话框：只列出当前平台支持的导出方式（HTML、图片、带评论的图片），复制 Markdown 各平台都有。
 * 导出成功后关闭对话框；失败时留在对话框里，可以换一种方式再试。
 */
@Composable
fun ExportDialogComponent(
    showDialog: Boolean,
    isHtmlExportSupported: Boolean,
    isImageExportSupported: Boolean,
    onDismiss: () -> Unit,
    onExportHtml: suspend (includeAppAttribution: Boolean, onComplete: (Boolean) -> Unit) -> Unit,
    onExportImage: suspend (includeAppAttribution: Boolean, onComplete: (Boolean) -> Unit) -> Unit,
    onExportMarkdown: () -> Unit,
    onExportImageWithComments: suspend (
        commentCount: Int,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) -> Unit,
) {
    if (showDialog) {
        val coroutineScope = rememberCoroutineScope()
        var commentCount by remember { mutableIntStateOf(3) }
        var isExporting by remember { mutableStateOf(false) }
        var includeAppAttribution by remember { mutableStateOf(true) }
        val export: (suspend (onComplete: (Boolean) -> Unit) -> Unit) -> Unit = { action ->
            if (!isExporting) {
                isExporting = true
                coroutineScope.launch {
                    action { success ->
                        isExporting = false
                        if (success) onDismiss()
                    }
                }
            }
        }
        val options = buildList {
            if (isHtmlExportSupported) {
                add(
                    ExportOption(AppIcons.Html, "导出为 HTML", "图片内联为 data URL，便于离线保存") {
                        export { onExportHtml(includeAppAttribution, it) }
                    },
                )
            }
            if (isImageExportSupported) {
                add(ExportOption(AppIcons.Image, "导出为图片", null) { export { onExportImage(includeAppAttribution, it) } })
            }
            add(
                ExportOption(AppIcons.ContentCopy, "复制 Markdown", null) {
                    onExportMarkdown()
                    onDismiss()
                },
            )
        }

        AlertDialog(
            onDismissRequest = { if (!isExporting) onDismiss() },
            icon = { Icon(AppIcons.Download, contentDescription = null) },
            title = { Text("导出文章") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEachIndexed { index, option ->
                        GroupedListItem(
                            index = index,
                            count = options.size,
                            onClick = option.onClick,
                            enabled = !isExporting,
                            leadingContent = { Icon(option.icon, contentDescription = null) },
                            supportingContent = option.description?.let { { Text(it) } },
                        ) {
                            Text(option.title)
                        }
                    }
                    if (isImageExportSupported) {
                        Text(
                            "带评论导出",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                        )
                        Text(
                            "包含评论 $commentCount 条",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = commentCount.toFloat(),
                            onValueChange = { commentCount = it.roundToInt() },
                            valueRange = 0f..10f,
                            steps = 9,
                            enabled = !isExporting,
                        )
                        GroupedListItem(
                            index = 0,
                            count = 1,
                            onClick = { export { onExportImageWithComments(commentCount, includeAppAttribution, it) } },
                            enabled = !isExporting,
                            leadingContent = { Icon(AppIcons.Comment, contentDescription = null) },
                        ) {
                            Text("导出图片（含评论）")
                        }
                    }
                    if (isHtmlExportSupported || isImageExportSupported) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(MaterialTheme.shapes.small)
                                .toggleable(
                                    value = includeAppAttribution,
                                    enabled = !isExporting,
                                    role = Role.Checkbox,
                                    onValueChange = { includeAppAttribution = it },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = includeAppAttribution, onCheckedChange = null, enabled = !isExporting)
                            Text(
                                "在导出底部加入 Zhihu-Hyperion 开源项目说明",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    if (isExporting) {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                "导出中…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss, enabled = !isExporting) {
                    Text("取消")
                }
            },
        )
    }
}
