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

package com.github.zly2006.zhihu.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberArticleTtsState(): TtsState {
    val state by AndroidReadingPlayerBridge.state.collectAsState()
    return when (state.status) {
        ReadingPlaybackStatus.Idle -> TtsState.Ready
        ReadingPlaybackStatus.Initializing -> TtsState.Initializing
        ReadingPlaybackStatus.Loading -> TtsState.LoadingText
        ReadingPlaybackStatus.Playing -> TtsState.Speaking
        ReadingPlaybackStatus.Paused -> TtsState.Paused
        ReadingPlaybackStatus.Error -> TtsState.Error
    }
}

@Composable
actual fun rememberArticleSpeechToggler(): ArticleSpeechToggler {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val ttsState = rememberArticleTtsState()
    return remember(context, coroutineScope, userMessages, ttsState) {
        object : ArticleSpeechToggler {
            override fun invoke(title: String, content: String) {
                if (ttsState.isSpeaking) {
                    context.startService(ContentReadingService.commandIntent(context, ContentReadingService.ACTION_STOP))
                } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val textToRead = articleSpeechText(title, content)
                                withContext(Dispatchers.Main) {
                                    if (textToRead.isNotBlank()) {
                                        AndroidReadingPlayerBridge.start(
                                            context,
                                            ReadingStartRequest(
                                                queue = listOf(
                                                    ReadingQueueItem(
                                                        contentType = ReadingContentType.Article,
                                                        id = title.hashCode().toLong() and 0xffffffffL,
                                                        title = title,
                                                        bodyHtml = textToRead,
                                                    ),
                                                ),
                                                preferences = ReadingPreferences(
                                                    fieldOrder = listOf(ReadingTemplateField.Body),
                                                    enabledFields = setOf(ReadingTemplateField.Body),
                                                    queueLimit = 1,
                                                    transitionText = "",
                                                ),
                                                playbackSpeed = loadReadingPlaybackSpeed(androidSettingsStore(context)),
                                            ),
                                        )
                                    }
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            userMessages.showMessage("朗读失败：${e.message}")
                        }
                    }
                }
            }
        }
    }
}
