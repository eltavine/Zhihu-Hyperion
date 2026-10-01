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

package com.github.zly2006.zhihu.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.data.ZhihuJson
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal class LensVideo(
    val coverUrl: String? = null,
    /** 以清晰度（FHD、HD、SD、LD）为键。 */
    val playlist: Map<String, LensVideoStream> = emptyMap(),
)

@Serializable
internal class LensVideoStream(
    val playUrl: String,
    val bitrate: Double = 0.0,
)

internal sealed interface VideoState {
    data object Loading : VideoState

    class Ready(
        val playUrl: String,
        val coverUrl: String?,
    ) : VideoState

    class Failed(
        val message: String,
    ) : VideoState
}

/** lens 接口只凭视频 id 就能给出播放地址，视频页因此不依赖打开它的回答或文章。 */
internal class VideoViewModel(
    private val videoId: Long,
    private val accountStore: ZhihuAccountStore,
) : ViewModel() {
    var state by mutableStateOf<VideoState>(VideoState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = VideoState.Loading
        viewModelScope.launch {
            state = try {
                val response = ZhihuJson.json
                    .parseToJsonElement(
                        accountStore.client
                            .httpClient()
                            .get("https://lens.zhihu.com/api/v4/videos/$videoId")
                            .bodyAsText(),
                    ).jsonObject
                val error = response["error"]
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonPrimitive
                    ?.content
                if (error != null) {
                    VideoState.Failed(error)
                } else {
                    val video = ZhihuJson.decodeJson<LensVideo>(response)
                    // 与原播放器一致，播放码率最高的清晰度。
                    video.playlist.values
                        .maxByOrNull { it.bitrate }
                        ?.let { VideoState.Ready(it.playUrl, video.coverUrl) }
                        ?: VideoState.Failed("视频没有可播放的清晰度")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                VideoState.Failed("视频加载失败：${e.message}")
            }
        }
    }
}
