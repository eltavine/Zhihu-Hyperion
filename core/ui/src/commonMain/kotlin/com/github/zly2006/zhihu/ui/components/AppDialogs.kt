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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.zly2006.zhihu.icons.AppIcon
import com.github.zly2006.zhihu.icons.Icon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.compose.koinInject

/** 对话框按钮：点击后先关闭对话框，再执行 [onClick]。 */
class AppDialogAction(
    val label: String,
    val onClick: () -> Unit = {},
)

/** 一个应用级对话框。[key] 相同的请求在关闭前只显示一次，之后到达的同 key 请求直接丢弃。 */
class AppDialogRequest(
    val key: String,
    val title: String,
    val text: String,
    val confirm: AppDialogAction,
    val dismiss: AppDialogAction? = null,
    val icon: AppIcon? = null,
)

/**
 * 不属于某个页面的对话框，例如登录过期、请求出错和连续浏览提醒，由进程级的这一个队列依次显示。
 *
 * 多个请求同时失败时只弹一次同类对话框；请求可以来自任意线程，[AppDialogHost] 在应用根部渲染队首。
 */
class AppDialogQueue {
    private val queued = MutableStateFlow<List<AppDialogRequest>>(emptyList())
    val requests: StateFlow<List<AppDialogRequest>> = queued.asStateFlow()

    fun show(request: AppDialogRequest) = queued.update { requests ->
        if (requests.any { it.key == request.key }) requests else requests + request
    }

    fun dismiss(key: String) = queued.update { requests -> requests.filterNot { it.key == key } }
}

/** 以 Material 3 对话框显示 [queue] 的队首；正文可滚动，崩溃堆栈这类长文本也能完整查看。 */
@Composable
fun AppDialogHost(queue: AppDialogQueue = koinInject()) {
    val requests by queue.requests.collectAsState()
    val request = requests.firstOrNull() ?: return
    AlertDialog(
        onDismissRequest = { queue.dismiss(request.key) },
        modifier = Modifier.testTag("app_dialog_${request.key}"),
        icon = request.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
        title = { Text(request.title) },
        text = { Text(request.text, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = {
            TextButton(onClick = {
                queue.dismiss(request.key)
                request.confirm.onClick()
            }) { Text(request.confirm.label) }
        },
        dismissButton = request.dismiss?.let { action ->
            {
                TextButton(onClick = {
                    queue.dismiss(request.key)
                    action.onClick()
                }) { Text(action.label) }
            }
        },
    )
}
