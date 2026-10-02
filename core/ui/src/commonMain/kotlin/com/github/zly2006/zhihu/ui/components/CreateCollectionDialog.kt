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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon

@Composable
fun CreateCollectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, Boolean) -> Unit,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
) {
    if (showDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isPublic by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) {
                    onDismiss()
                }
            },
            modifier = Modifier.testTag(CREATE_COLLECTION_DIALOG_TAG),
            icon = { Icon(AppIcons.Bookmarks, contentDescription = null) },
            title = { Text("新建收藏夹") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("收藏夹名称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CREATE_COLLECTION_TITLE_INPUT_TAG),
                        enabled = !isSubmitting,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting,
                        maxLines = 3,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .toggleable(
                                value = isPublic,
                                enabled = !isSubmitting,
                                role = Role.Checkbox,
                                onValueChange = { isPublic = it },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = isPublic, onCheckedChange = null, enabled = !isSubmitting)
                        Text(
                            text = "公开收藏夹",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(title.trim(), description.trim(), isPublic) },
                    enabled = title.isNotBlank() && !isSubmitting,
                    modifier = Modifier.testTag(CREATE_COLLECTION_CONFIRM_TAG),
                ) {
                    Text(if (isSubmitting) "创建中…" else "创建")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                    Text("取消")
                }
            },
        )
    }
}

private const val CREATE_COLLECTION_DIALOG_TAG = "create_collection_dialog"
private const val CREATE_COLLECTION_TITLE_INPUT_TAG = "create_collection_title_input"
private const val CREATE_COLLECTION_CONFIRM_TAG = "create_collection_confirm"
