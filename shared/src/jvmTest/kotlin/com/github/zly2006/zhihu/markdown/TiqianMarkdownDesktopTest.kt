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

package com.github.zly2006.zhihu.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.platform.MapSettingsStore
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.AtomicTextFile
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.io.files.Path
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TiqianMarkdownDesktopTest {
    /** 系统里没有提椠认得的字体时（Windows 默认如此），开启提椠渲染器后文章正文一个字都不显示。 */
    @Test
    fun articleTextStaysVisibleWithTiqianRendererEnabled() {
        val accountStore = ZhihuAccountStore(
            ZhihuAccountRepository(AtomicTextFile(Path(createTempDirectory().toString(), "account.json"))),
            MockEngine { respond("") },
        )
        val koin = koinApplication {
            modules(
                module {
                    single { accountStore }
                    single<SettingsStore> { MapSettingsStore() }
                    single<ZhihuApiEnvironment> {
                        object : ZhihuApiEnvironment {
                            override fun httpClient() = error("渲染正文不发请求")

                            override fun authenticatedCookies() = emptyMap<String, String>()

                            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
                        }
                    }
                },
            )
        }
        try {
            runComposeUiTest {
                setContent {
                    KoinIsolatedContext(koin) {
                        CompositionLocalProvider(LocalNavigator provides Navigator(onNavigate = {}, onNavigateBack = {})) {
                            Box(Modifier.size(360.dp, 120.dp).background(Color.White)) {
                                RenderMarkdown(
                                    html = "<p>知乎正文 Zhihu article text</p>",
                                    enableScroll = false,
                                    useTiqianRenderer = true,
                                )
                            }
                        }
                    }
                }

                val pixels = onRoot().captureToImage().toPixelMap()
                val inkPixels = (0 until pixels.width).sumOf { x ->
                    (0 until pixels.height).count { y -> pixels[x, y].luminance() < 0.5f }
                }
                assertTrue(inkPixels > 100, "正文区域只有 $inkPixels 个深色像素，文字没有画出来")
            }
        } finally {
            koin.close()
            accountStore.close()
        }
    }
}
