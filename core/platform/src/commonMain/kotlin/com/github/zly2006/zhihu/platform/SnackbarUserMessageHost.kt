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

package com.github.zly2006.zhihu.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

const val USER_MESSAGE_HOST_TAG = "user_message_host"

private class QueuedUserMessage(
    val text: String,
    val duration: UserMessageDuration,
)

private val queuedUserMessages = Channel<QueuedUserMessage>(capacity = Channel.UNLIMITED)

/** 桌面与 iOS 的应用内提示：消息排进队列，由 [SnackbarUserMessageHost] 显示。Android 用 Toast，不经过这里。 */
object SnackbarUserMessages : UserMessageSink {
    override fun showShortMessage(message: String) = enqueue(message, UserMessageDuration.Short)

    override fun showLongMessage(message: String) = enqueue(message, UserMessageDuration.Long)

    private fun enqueue(message: String, duration: UserMessageDuration) {
        println(message)
        check(queuedUserMessages.trySend(QueuedUserMessage(message, duration)).isSuccess) {
            "user message queue is unavailable"
        }
    }
}

/** 把 [SnackbarUserMessages] 收到的消息显示成 Material Snackbar，宿主在内容外包一层即可。 */
@Composable
fun SnackbarUserMessageHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarHostState) {
        queuedUserMessages.receiveAsFlow().collect { message ->
            snackbarHostState.showSnackbar(
                message = message.text,
                duration = when (message.duration) {
                    UserMessageDuration.Short -> SnackbarDuration.Short
                    UserMessageDuration.Long -> SnackbarDuration.Long
                },
            )
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        content()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .testTag(USER_MESSAGE_HOST_TAG),
        )
    }
}
