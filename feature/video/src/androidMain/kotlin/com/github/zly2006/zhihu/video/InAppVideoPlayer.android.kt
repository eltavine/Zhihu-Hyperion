/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
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

@file:OptIn(UnstableApi::class)

package com.github.zly2006.zhihu.video

import android.app.DownloadManager
import android.content.Context
import android.content.res.Configuration
import android.media.MediaMetadataRetriever
import android.os.Environment
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.indicator.PositionAndDurationText
import androidx.media3.ui.compose.material3.indicator.ProgressSlider
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import androidx.media3.ui.compose.text.ErrorText
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.util.saveBitmapToGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

actual val isInAppVideoPlaybackSupported: Boolean = true

/** 播放器跟随返回栈条目存活，旋转屏幕重建 Activity 时继续播放。 */
private class VideoPlayerHolder(
    context: Context,
    playUrl: String,
    private val settings: SettingsStore,
    private val progressKey: String,
) : ViewModel() {
    val player: ExoPlayer = ExoPlayer
        .Builder(context)
        .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(playUrl), settings.getLong(progressKey, 0))
            prepare()
            playWhenReady = true
        }

    /** 播放超过 1 秒才记住进度；离结尾不足 3 秒视为看完，下次从头播放。 */
    fun saveProgress() {
        val position = player.currentPosition
        val duration = player.duration
        if (position <= 1000 || duration == C.TIME_UNSET) return
        if (duration - position > 3000) settings.putLong(progressKey, position) else settings.remove(progressKey)
    }

    override fun onCleared() {
        saveProgress()
        player.release()
    }
}

@Composable
actual fun InAppVideoPlayer(
    videoId: Long,
    playUrl: String,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val settings = koinInject<SettingsStore>()
    val holder = viewModel { VideoPlayerHolder(context.applicationContext, playUrl, settings, "video_progress_$videoId") }
    val player = holder.player
    val playPause = rememberPlayPauseButtonState(player)
    val speed = rememberPlaybackSpeedState(player)
    val userMessages = rememberUserMessageSink()
    val scope = rememberCoroutineScope()
    var controlsVisible by remember { mutableStateOf(false) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var lockHintVisible by remember { mutableStateOf(false) }
    var fastForwarding by remember { mutableStateOf(false) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var seekPreview by remember { mutableLongStateOf(0L) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) player.pause()
        holder.saveProgress()
    }
    DisposableEffect(activity, isLandscape) {
        val insets = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (isLandscape) {
            insets?.hide(WindowInsetsCompat.Type.systemBars())
            insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose { insets?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    LaunchedEffect(controlsVisible, playPause.showPlay, locked, speedMenuExpanded) {
        if (locked || speedMenuExpanded) return@LaunchedEffect
        if (playPause.showPlay) {
            controlsVisible = true
        } else if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }
    LaunchedEffect(lockHintVisible) {
        if (lockHintVisible) {
            delay(3000)
            lockHintVisible = false
        }
    }

    Box(if (playPause.showPlay) Modifier.fillMaxSize() else Modifier.fillMaxSize().keepScreenOn()) {
        ContentFrame(player, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(locked) {
                    detectTapGestures(
                        onTap = { if (locked) lockHintVisible = true else controlsVisible = !controlsVisible },
                        onDoubleTap = if (locked) null else { _ -> Util.handlePlayPauseButtonAction(player) },
                        onLongPress = if (locked) {
                            null
                        } else {
                            { _ ->
                                speed.temporarilyOverrideSpeedWith(2f)
                                fastForwarding = true
                            }
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (fastForwarding) {
                                speed.restoreOverriddenSpeed()
                                fastForwarding = false
                            }
                        },
                    )
                }.pointerInput(locked) {
                    if (locked) return@pointerInput
                    var dragX = 0f
                    var seeking = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragX = 0f
                            seeking = !fastForwarding
                        },
                        onDragEnd = {
                            val duration = player.duration
                            if (seeking && duration > 0) {
                                player.seekTo((player.currentPosition + seekPreview).coerceIn(0, duration))
                                controlsVisible = true
                            }
                            seekPreview = 0
                        },
                        onDragCancel = { seekPreview = 0 },
                    ) { change, dragAmount ->
                        if (!seeking) return@detectHorizontalDragGestures
                        change.consume()
                        dragX += dragAmount
                        // 拖过整个画面宽度对应前后 15 秒。
                        seekPreview = (dragX / size.width * 15_000).toLong()
                    }
                },
        )
        ErrorText(player) {
            if (error != null) Text("视频播放失败", Modifier.align(Alignment.Center).padding(24.dp), color = Color.White)
        }

        AnimatedVisibility(
            visible = fastForwarding,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp),
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.35f)) {
                Text(
                    "2x",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }

        val duration = player.duration
        if (seekPreview != 0L && duration > 0) {
            Surface(
                Modifier.align(Alignment.Center).padding(bottom = 80.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f),
            ) {
                Text(
                    "${if (seekPreview > 0) "+" else ""}${seekPreview / 1000}s  " +
                        Util.getStringForTime((player.currentPosition + seekPreview).coerceIn(0, duration)),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (controlsVisible && !locked) {
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = Color.Black.copy(alpha = 0.55f)) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    ProgressSlider(player, Modifier.fillMaxWidth())
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayPauseButton(
                            player,
                            Modifier.size(36.dp),
                            iconSize = 20.dp,
                            contentDescription = { if (showPlay) "播放" else "暂停" },
                            tint = Color.White,
                        )
                        ProvideTextStyle(TextStyle(fontSize = 12.sp)) {
                            PositionAndDurationText(player, color = Color.White.copy(alpha = 0.8f))
                        }
                        IconButton(
                            onClick = {
                                val position = player.currentPosition
                                scope.launch {
                                    val saved = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val retriever = MediaMetadataRetriever()
                                            val frame = try {
                                                retriever.setDataSource(playUrl, emptyMap())
                                                retriever.getFrameAtTime(position * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                            } finally {
                                                retriever.release()
                                            }
                                            saveBitmapToGallery(context.applicationContext, "zhihu_${System.currentTimeMillis()}.jpg", checkNotNull(frame))
                                        }
                                    }
                                    userMessages.showShortMessage(saved.fold({ "截图已保存" }, { "截图失败: ${it.message}" }))
                                }
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.CameraAlt, "截图", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                locked = true
                                controlsVisible = false
                                lockHintVisible = true
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.LockOpen, "锁定", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(
                                    DownloadManager
                                        .Request(playUrl.toUri())
                                        .setDestinationInExternalPublicDir(
                                            Environment.DIRECTORY_DOWNLOADS,
                                            "zhihu_video_${System.currentTimeMillis()}.mp4",
                                        ).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED),
                                )
                                userMessages.showShortMessage("开始下载")
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.Download, "下载", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Box {
                            Text(
                                "${speed.playbackSpeed}x",
                                color = if (speed.playbackSpeed != 1f) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp).clickable { speedMenuExpanded = true },
                            )
                            DropdownMenu(
                                expanded = speedMenuExpanded,
                                onDismissRequest = { speedMenuExpanded = false },
                                modifier = Modifier.background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp)),
                            ) {
                                floatArrayOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { value ->
                                    val selected = value == speed.playbackSpeed
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${value}x",
                                                color = if (selected) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.75f),
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            speed.updatePlaybackSpeed(value)
                                            speedMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (locked && lockHintVisible) {
            Surface(
                onClick = {
                    locked = false
                    controlsVisible = true
                    lockHintVisible = false
                },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(40.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, "解锁", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
