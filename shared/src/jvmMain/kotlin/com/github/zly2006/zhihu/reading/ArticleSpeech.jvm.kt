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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberArticleTtsState(): TtsState = DesktopArticleSpeechController.currentTtsState

@Composable
actual fun rememberArticleSpeechToggler(): ArticleSpeechToggler {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    return remember(userMessages, coroutineScope) {
        object : ArticleSpeechToggler {
            override fun invoke(title: String, content: String) {
                DesktopArticleSpeechController.toggleSpeech(title, content, coroutineScope, userMessages)
            }
        }
    }
}

private object DesktopArticleSpeechController {
    private var speechProcess: Process? = null
    var currentTtsState by mutableStateOf(
        if (isDesktopSpeechCommandAvailable()) TtsState.Ready else TtsState.Error,
    )
        private set

    fun toggleSpeech(
        title: String,
        content: String,
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        userMessages: UserMessageSink,
    ) {
        if (currentTtsState.isSpeaking) {
            stopSpeaking()
        } else if (currentTtsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
            coroutineScope.launch {
                try {
                    val textToRead = withContext(Dispatchers.IO) {
                        articleSpeechText(title, content)
                    }
                    if (textToRead.isNotBlank()) {
                        speakText(textToRead, title, userMessages)
                    }
                } catch (e: Exception) {
                    currentTtsState = TtsState.Error
                    userMessages.showMessage("朗读失败：${e.message}")
                }
            }
        }
    }

    private suspend fun speakText(
        text: String,
        title: String,
        userMessages: UserMessageSink,
    ) {
        currentTtsState = TtsState.LoadingText
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder("say")
                .redirectErrorStream(true)
                .start()
        }
        speechProcess = process
        currentTtsState = TtsState.Speaking
        userMessages.showMessage("开始朗读：$title")
        val exitCode = withContext(Dispatchers.IO) {
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(text)
            }
            process.waitFor()
        }
        if (speechProcess == process) {
            speechProcess = null
            currentTtsState = if (exitCode == 0) TtsState.Ready else TtsState.Error
        }
    }

    private fun stopSpeaking() {
        speechProcess?.destroy()
        speechProcess = null
        currentTtsState = TtsState.Ready
    }
}

private fun isDesktopSpeechCommandAvailable(): Boolean =
    runCatching {
        ProcessBuilder("sh", "-c", "command -v say >/dev/null 2>&1")
            .start()
            .waitFor() == 0
    }.getOrDefault(false)
