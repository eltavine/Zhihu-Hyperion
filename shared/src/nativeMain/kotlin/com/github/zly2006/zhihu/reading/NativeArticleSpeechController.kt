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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

/** 进入朗读前准备平台音频会话；iOS 需要让朗读不受静音键影响，macOS 没有音频会话。 */
internal expect fun prepareSpeechAudioSession()

/** macOS 与 iOS 共用的文章朗读：AVFoundation 的 AVSpeechSynthesizer 在两个平台上行为一致。 */
@OptIn(ExperimentalForeignApi::class)
internal object NativeArticleSpeechController {
    private val speechDelegate = SpeechDelegate()
    private val synthesizer = AVSpeechSynthesizer().apply { delegate = speechDelegate }
    private var state by mutableStateOf(TtsState.Ready)

    val currentState: TtsState
        get() = state

    fun startSpeaking(text: String): Boolean {
        if (text.isBlank() || synthesizer.speaking) return false
        prepareSpeechAudioSession()
        val utterance = AVSpeechUtterance(string = text).apply {
            // 系统没有安装中文语音时返回 null，这时沿用系统默认语音。
            AVSpeechSynthesisVoice.voiceWithLanguage("zh-CN")?.let { voice = it }
        }
        synthesizer.speakUtterance(utterance)
        state = TtsState.Speaking
        return true
    }

    fun stopSpeaking() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        state = TtsState.Ready
    }

    fun onUtteranceEnded() {
        state = TtsState.Ready
    }
}

private class SpeechDelegate :
    NSObject(),
    AVSpeechSynthesizerDelegateProtocol {
    @ObjCSignatureOverride
    override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
        NativeArticleSpeechController.onUtteranceEnded()
    }

    @ObjCSignatureOverride
    override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) {
        NativeArticleSpeechController.onUtteranceEnded()
    }
}
