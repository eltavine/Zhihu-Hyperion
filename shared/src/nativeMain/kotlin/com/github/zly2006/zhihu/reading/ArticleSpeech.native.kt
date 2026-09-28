/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberArticleTtsState(): TtsState = NativeArticleSpeechController.currentState

@Composable
actual fun rememberArticleSpeechToggler(): ArticleSpeechToggler {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val ttsState = NativeArticleSpeechController.currentState
    return remember(userMessages, coroutineScope, ttsState) {
        object : ArticleSpeechToggler {
            override fun invoke(title: String, content: String) {
                if (ttsState.isSpeaking) {
                    NativeArticleSpeechController.stopSpeaking()
                } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    coroutineScope.launch {
                        try {
                            val textToRead = withContext(Dispatchers.Default) {
                                articleSpeechText(title, content)
                            }
                            if (textToRead.isNotBlank()) {
                                if (NativeArticleSpeechController.startSpeaking(textToRead)) {
                                    userMessages.showMessage("开始朗读：$title")
                                } else {
                                    userMessages.showMessage("朗读启动失败")
                                }
                            }
                        } catch (e: Exception) {
                            userMessages.showMessage("朗读失败：${e.message}")
                        }
                    }
                }
            }
        }
    }
}
