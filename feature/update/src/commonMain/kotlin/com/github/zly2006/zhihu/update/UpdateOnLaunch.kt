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

package com.github.zly2006.zhihu.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon
import org.koin.compose.koinInject

/**
 * 放在应用外壳里。还没决定是否使用 GitHub 加速时先弹窗询问，回答之后才做启动时的更新检查；
 * 直接关掉弹窗不算回答，本次运行不再询问也不检查，下次冷启动再问。
 */
@Composable
fun UpdateOnLaunch() {
    val controller = koinInject<UpdateController>()
    val acceleration by controller.acceleration.collectAsState()
    var offerDismissed by rememberSaveable { mutableStateOf(false) }
    if (acceleration != GitHubAcceleration.UNDECIDED) {
        LaunchedEffect(Unit) { controller.checkOnLaunch() }
    } else if (!offerDismissed) {
        AlertDialog(
            onDismissRequest = { offerDismissed = true },
            icon = { Icon(AppIcons.Bolt, contentDescription = null) },
            title = { Text("开启 GitHub 加速？") },
            text = {
                Text(
                    "检查和下载更新需要访问 GitHub，国内网络直连可能很慢甚至失败。" +
                        "开启后将通过第三方加速服务 gh-proxy.com 中转。之后可随时在“系统与更新”中更改。",
                )
            },
            confirmButton = {
                TextButton(onClick = { controller.setAcceleration(GitHubAcceleration.ENABLED) }) { Text("开启加速") }
            },
            dismissButton = {
                TextButton(onClick = { controller.setAcceleration(GitHubAcceleration.DISABLED) }) { Text("暂不开启") }
            },
        )
    }
}
