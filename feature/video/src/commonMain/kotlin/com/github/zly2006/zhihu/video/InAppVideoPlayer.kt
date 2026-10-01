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

import androidx.compose.runtime.Composable

/** 不支持的平台在视频页交给浏览器播放。 */
expect val isInAppVideoPlaybackSupported: Boolean

/** 全屏播放 [playUrl]，按 [videoId] 记住进度，下次打开同一视频时继续播放。 */
@Composable
expect fun InAppVideoPlayer(
    videoId: Long,
    playUrl: String,
)
