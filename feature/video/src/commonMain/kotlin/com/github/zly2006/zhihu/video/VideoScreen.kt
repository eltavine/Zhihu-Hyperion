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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.platform.rememberExternalUrlOpener
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(video: Video) {
    val accountStore = koinInject<ZhihuAccountStore>()
    val viewModel = viewModel { VideoViewModel(video.id, accountStore) }
    val navigator = LocalNavigator.current
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = viewModel.state) {
            VideoState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)

            is VideoState.Failed -> Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message, color = Color.White)
                TextButton(onClick = viewModel::load) { Text("重试") }
            }

            is VideoState.Ready -> if (isInAppVideoPlaybackSupported) {
                InAppVideoPlayer(video.id, state.playUrl)
            } else {
                val openExternalUrl = rememberExternalUrlOpener()
                Box(Modifier.fillMaxSize().clickable { openExternalUrl(state.playUrl) }) {
                    AsyncImage(model = state.coverUrl, contentDescription = "视频封面", modifier = Modifier.fillMaxSize())
                    Column(
                        Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.56f)) {
                            Icon(AppIcons.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(16.dp))
                        }
                        Text("在浏览器中播放", color = Color.White)
                    }
                }
            }
        }
        if (maxWidth > maxHeight) {
            Surface(
                onClick = navigator.onNavigateBack,
                modifier = Modifier.padding(12.dp).size(40.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        } else {
            TopAppBar(
                title = { Text("视频播放") },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(AppIcons.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E).copy(alpha = 0.85f),
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White,
                ),
            )
        }
    }
}
