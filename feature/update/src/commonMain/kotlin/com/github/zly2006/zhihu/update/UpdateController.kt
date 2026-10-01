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

import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.time.Clock

/** 是否接收 Nightly；设置页开关、设置同步和更新检查共用。 */
const val CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY = "checkNightlyUpdates"

/** 保存 [GitHubAcceleration] 的名称；没有保存过即 [GitHubAcceleration.UNDECIDED]。 */
const val GITHUB_ACCELERATION_PREFERENCE_KEY = "githubAcceleration"

private const val LAST_LAUNCH_CHECK_PREFERENCE_KEY = "lastUpdateCheck"
private const val SKIPPED_VERSION_CODE_PREFERENCE_KEY = "skippedUpdateVersionCode"
private const val LAUNCH_CHECK_INTERVAL_MILLIS = 3 * 60 * 60 * 1000L
private const val TAG = "UpdateController"

internal val manifestJson = Json { ignoreUnknownKeys = true }

val updateModule = module {
    single { UpdateController(get(), get(), get()) }
}

/**
 * 进程内唯一的更新状态：读取当前渠道的 update.json，用 versionCode 与 [installedBuild] 比较，
 * 在支持的平台上下载安装包。请求用 Koin 的共享引擎新建客户端，不带知乎账号的 Cookie。
 */
class UpdateController(
    private val settings: SettingsStore,
    private val engine: HttpClientEngine,
    private val installedBuild: InstalledBuild,
) {
    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = mutableState.asStateFlow()

    private val mutableAcceleration = MutableStateFlow(
        GitHubAcceleration.entries.firstOrNull { it.name == settings.getStringOrNull(GITHUB_ACCELERATION_PREFERENCE_KEY) }
            ?: GitHubAcceleration.UNDECIDED,
    )
    val acceleration: StateFlow<GitHubAcceleration> = mutableAcceleration.asStateFlow()

    fun setAcceleration(acceleration: GitHubAcceleration) {
        settings.putString(GITHUB_ACCELERATION_PREFERENCE_KEY, acceleration.name)
        mutableAcceleration.value = acceleration
    }

    /** 启动时的检查：距上次不足三小时就不再请求，用户跳过的版本也不再提示。 */
    suspend fun checkOnLaunch() {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - settings.getLong(LAST_LAUNCH_CHECK_PREFERENCE_KEY, 0L) < LAUNCH_CHECK_INTERVAL_MILLIS) return
        settings.putLong(LAST_LAUNCH_CHECK_PREFERENCE_KEY, now)
        refresh(skippedVersionCode = settings.getInt(SKIPPED_VERSION_CODE_PREFERENCE_KEY, 0))
    }

    /** 用户主动检查：忽略检查间隔和跳过的版本。 */
    suspend fun checkNow() = refresh(skippedVersionCode = 0)

    fun skip(update: AvailableUpdate) {
        settings.putInt(SKIPPED_VERSION_CODE_PREFERENCE_KEY, update.versionCode)
        mutableState.value = UpdateState.UpToDate
    }

    /** 下载并校验 [update] 的安装包；只能在 [isInAppUpdateInstallSupported] 的平台调用。 */
    suspend fun download(update: AvailableUpdate) {
        val asset = requireNotNull(update.asset) { "update.json 中没有本平台的安装包" }
        mutableState.value = UpdateState.Downloading(update)
        mutableState.value = try {
            HttpClient(engine).use { downloadUpdatePackage(it, asset) }
            UpdateState.Downloaded(update)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download ${update.displayVersion}", e)
            UpdateState.Failed(e.message ?: "下载失败")
        }
    }

    fun install() = installDownloadedUpdatePackage()

    private suspend fun refresh(skippedVersionCode: Int) {
        mutableState.value = UpdateState.Checking
        mutableState.value = try {
            fetchNewerBuild()
                ?.takeIf { it.versionCode > skippedVersionCode }
                ?.let(UpdateState::Available)
                ?: UpdateState.UpToDate
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            UpdateState.Failed(e.message ?: "检查更新失败")
        }
    }

    private suspend fun fetchNewerBuild(): AvailableUpdate? {
        val channel = if (settings.getBoolean(CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY, false)) UpdateChannel.NIGHTLY else UpdateChannel.STABLE
        val acceleration = acceleration.value
        val manifest = HttpClient(engine).use { client ->
            val response = client.get(acceleration.route(channel.manifestUrl))
            // A channel that has never been published, or the nightly while CI replaces it, has no update.json yet.
            if (response.status == HttpStatusCode.NotFound) return null
            check(response.status.isSuccess()) { "update.json 返回 HTTP ${response.status.value}" }
            manifestJson.decodeFromString<UpdateManifest>(response.bodyAsText())
        }
        if (manifest.versionCode <= installedBuild.versionCode) return null
        return AvailableUpdate(
            channel = channel,
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            notes = manifest.notes,
            releaseUrl = manifest.releaseUrl,
            asset = installedBuild.target
                ?.let { manifest.assets[it.key] }
                ?.let { it.copy(url = acceleration.route(it.url)) },
        )
    }
}
