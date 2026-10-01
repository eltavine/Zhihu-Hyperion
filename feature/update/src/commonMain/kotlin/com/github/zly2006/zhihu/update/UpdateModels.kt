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

package com.github.zly2006.zhihu.update

import kotlinx.serialization.Serializable

private const val RELEASES_URL = "https://github.com/eltavine/Zhihu-Hyperion/releases"

/**
 * CI 随每个 nightly 和正式版发布的 update.json，由 `.github/scripts/write_update_manifest.py` 生成。
 *
 * 已发布的客户端会一直读取这个文件：只能新增字段（旧客户端忽略不认识的键），不能改名、删除或改变已有字段的含义；
 * 需要不兼容的格式时应发布新的文件名。
 */
@Serializable
data class UpdateManifest(
    val versionName: String,
    val versionCode: Int,
    val commit: String,
    val releaseUrl: String,
    val notes: String = "",
    val assets: Map<String, UpdateAsset> = emptyMap(),
)

@Serializable
data class UpdateAsset(
    val url: String,
    val size: Long,
    val sha256: String,
)

/** 正式版的地址经过 GitHub 的 latest 跳转，始终指向最新的非预发布版本。 */
enum class UpdateChannel(
    internal val manifestUrl: String,
) {
    STABLE("$RELEASES_URL/latest/download/update.json"),
    NIGHTLY("$RELEASES_URL/download/nightly/update.json"),
}

/** 发布包的种类；[key] 是 update.json 中 `assets` 的键，与 CI 清单脚本里的键一一对应。 */
enum class UpdateTarget(
    val key: String,
) {
    ANDROID_LITE("android-lite"),
    ANDROID_FULL("android-full"),
    WINDOWS_X64("windows-x64"),
    LINUX_X64("linux-x64"),
    MACOS_ARM64("macos-arm64"),
}

/**
 * 正在运行的这份构建，由各平台的 Koin 模块从构建元数据创建。更新检查只用 [versionCode] 比较新旧，
 * 用 [target] 在 update.json 里挑本平台的安装包；CI 不发布安装包的平台（如在 macOS 上跑 JVM 桌面版）为 null。
 */
data class InstalledBuild(
    val versionName: String,
    val versionCode: Int,
    val commit: String,
    val target: UpdateTarget?,
)

/**
 * 比当前构建新的版本。[asset] 的地址已按当前的 [GitHubAcceleration] 改写；
 * update.json 没有本平台的安装包时为 null，只能去 [releaseUrl] 手动下载。
 */
data class AvailableUpdate(
    val channel: UpdateChannel,
    val versionName: String,
    val versionCode: Int,
    val notes: String,
    val releaseUrl: String,
    val asset: UpdateAsset?,
) {
    /** 每个 nightly 的 versionName 都相同，只能靠 versionCode 区分。 */
    val displayVersion: String
        get() = if (channel == UpdateChannel.NIGHTLY) "$versionName Nightly $versionCode" else versionName
}

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data object UpToDate : UpdateState

    data class Available(
        val update: AvailableUpdate,
    ) : UpdateState

    data class Downloading(
        val update: AvailableUpdate,
    ) : UpdateState

    data class Downloaded(
        val update: AvailableUpdate,
    ) : UpdateState

    data class Failed(
        val message: String,
    ) : UpdateState
}

/** 用户是否让更新请求经 gh-proxy.com 访问 GitHub；回答之前是 [UNDECIDED]，应用启动时会询问。 */
enum class GitHubAcceleration {
    UNDECIDED,
    ENABLED,
    DISABLED,
}

/**
 * gh-proxy.com 在服务端代取完整的 GitHub 地址，包括 `releases/latest` 和发布附件的跳转，
 * 用于直连 GitHub 很慢或失败的网络：https://gh-proxy.com/docs/github-accelerator
 */
internal fun GitHubAcceleration.route(gitHubUrl: String): String =
    if (this == GitHubAcceleration.ENABLED) "https://gh-proxy.com/$gitHubUrl" else gitHubUrl
