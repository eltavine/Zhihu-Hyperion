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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

const val NATIVE_USER_MESSAGE_HOST_TAG = "native_user_message_host"

data class NativeUserMessage(
    val text: String,
    val duration: UserMessageDuration,
)

internal val nativeUserMessages = Channel<NativeUserMessage>(capacity = Channel.UNLIMITED)

fun showNativeUserMessage(
    message: String,
    duration: UserMessageDuration = UserMessageDuration.Short,
) {
    println(message)
    check(nativeUserMessages.trySend(NativeUserMessage(message, duration)).isSuccess) {
        "native user message queue is unavailable"
    }
}

/** macOS 与 iOS 的应用内提示：把 [rememberUserMessageSink] 发出的消息显示成 Material Snackbar，宿主在内容外包一层即可。 */
@Composable
fun NativeUserMessageHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarHostState) {
        nativeUserMessages.receiveAsFlow().collect { message ->
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
                .testTag(NATIVE_USER_MESSAGE_HOST_TAG),
        )
    }
}

/**
 * 盖在主窗口上的界面（iOS 的看图页）显示自己的提示条：主窗口的 [NativeUserMessageHost] 被挡在下面，
 * 消息若仍进全局队列，用户在看图页里看不到“已保存”之类的结果。
 */
internal val LocalNativeUserMessageSink = staticCompositionLocalOf<UserMessageSink?> { null }

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = LocalNativeUserMessageSink.current ?: remember {
    object : UserMessageSink {
        override fun showShortMessage(message: String) = showNativeUserMessage(message)

        override fun showLongMessage(message: String) = showNativeUserMessage(message, UserMessageDuration.Long)
    }
}
