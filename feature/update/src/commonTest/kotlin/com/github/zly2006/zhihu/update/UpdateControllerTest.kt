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

import com.github.zly2006.zhihu.platform.MapSettingsStore
import com.github.zly2006.zhihu.platform.SettingsStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UpdateControllerTest {
    private val requests = mutableListOf<String>()
    private var status = HttpStatusCode.OK
    private var manifest = manifest(versionCode = 11)

    @Test
    fun nightlyThroughGhProxyRewritesManifestAndPackageUrls() = runTest {
        val settings = MapSettingsStore(mutableMapOf<String, Any>(CHECK_NIGHTLY_UPDATES_PREFERENCE_KEY to true))
        val controller = controller(settings)
        controller.setAcceleration(GitHubAcceleration.ENABLED)

        controller.checkNow()

        assertEquals(listOf("https://gh-proxy.com/$RELEASES/download/nightly/update.json"), requests)
        val update = assertIs<UpdateState.Available>(controller.state.value).update
        assertEquals("1.1.0 Nightly 11", update.displayVersion)
        assertEquals("https://gh-proxy.com/$RELEASES/download/nightly/zhihu-hyperion-lite.apk", update.asset?.url)
        assertEquals(GitHubAcceleration.ENABLED, controller(settings).acceleration.value)
    }

    @Test
    fun stableChannelComparesVersionCodesAndKeepsBuildsWithoutPackages() = runTest {
        val controller = controller(MapSettingsStore())
        manifest = manifest(versionCode = 10)

        controller.checkNow()

        assertEquals(listOf("$RELEASES/latest/download/update.json"), requests)
        assertEquals(UpdateState.UpToDate, controller.state.value)

        manifest = manifest(versionCode = 12, assets = "")
        controller.checkNow()

        val update = assertIs<UpdateState.Available>(controller.state.value).update
        assertEquals("1.1.0", update.displayVersion)
        assertNull(update.asset)
    }

    @Test
    fun launchCheckWaitsBetweenChecksAndHonoursTheSkippedVersion() = runTest {
        val controller = controller(MapSettingsStore())
        controller.checkNow()
        controller.skip(assertIs<UpdateState.Available>(controller.state.value).update)

        controller.checkOnLaunch()
        controller.checkOnLaunch()

        assertEquals(UpdateState.UpToDate, controller.state.value)
        assertEquals(2, requests.size)
    }

    @Test
    fun unpublishedChannelIsUpToDateWhileServerErrorsFail() = runTest {
        val controller = controller(MapSettingsStore())
        status = HttpStatusCode.NotFound

        controller.checkNow()

        assertEquals(UpdateState.UpToDate, controller.state.value)

        status = HttpStatusCode.BadGateway
        controller.checkNow()

        assertEquals(UpdateState.Failed("update.json 返回 HTTP 502"), controller.state.value)
    }

    private fun controller(settings: SettingsStore) = UpdateController(
        settings = settings,
        engine = MockEngine { request ->
            requests += request.url.toString()
            respond(manifest, status)
        },
        installedBuild = InstalledBuild(versionName = "1.0.0", versionCode = 10, commit = "aaaaaaa", target = UpdateTarget.ANDROID_LITE),
    )

    private fun manifest(
        versionCode: Int,
        assets: String = """"android-lite":{"url":"$RELEASES/download/nightly/zhihu-hyperion-lite.apk","size":5,"sha256":"00"}""",
    ) =
        """
        {"versionName":"1.1.0","versionCode":$versionCode,"commit":"bbbbbbb","releaseUrl":"$RELEASES/tag/nightly",
         "notes":"修复","assets":{$assets},"addedLater":true}
        """.trimIndent()

    private companion object {
        const val RELEASES = "https://github.com/eltavine/Zhihu-Hyperion/releases"
    }
}
