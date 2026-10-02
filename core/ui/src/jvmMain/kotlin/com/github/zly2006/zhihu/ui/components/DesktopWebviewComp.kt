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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.github.zly2006.zhihu.account.ZHIHU_DESKTOP_USER_AGENT
import com.github.zly2006.zhihu.account.ZHIHU_HOME_URL
import com.github.zly2006.zhihu.account.parseCookieAssignments
import com.sun.webkit.network.CookieManager
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import java.net.CookieHandler
import java.net.URI

/**
 * 桌面端内嵌 WebView 组件，使用 JavaFX WebView + SwingPanel 嵌入 Compose Desktop。
 *
 * 对应 Android 端的 WebviewComp，用于网页登录和知乎风控验证页面。每个 WebView 打开时 cookie 存储只含
 * [initialCookies]，不沿用之前页面留下的会话。
 *
 * @param url 需要加载的页面 URL
 * @param modifier Compose Modifier
 * @param userAgent 浏览器 User-Agent，默认使用知乎桌面端 UA
 * @param initialCookies 页面所在会话的 cookies，在加载前写入 cookie 存储
 * @param onPageFinished 页面加载完成时的回调，携带当前 URL
 * @param onCookiesChanged 页面加载完成后回传知乎域名下的全部 cookies（包括 HttpOnly 的 z_c0）
 */
@Composable
fun DesktopWebviewComp(
    url: String?,
    modifier: Modifier = Modifier,
    userAgent: String = ZHIHU_DESKTOP_USER_AGENT,
    initialCookies: Map<String, String> = emptyMap(),
    onPageFinished: (url: String?) -> Unit = {},
    onCookiesChanged: (Map<String, String>) -> Unit = {},
) {
    var engineState by remember { mutableStateOf<WebEngine?>(null) }
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnCookiesChanged by rememberUpdatedState(onCookiesChanged)

    DisposableEffect(Unit) {
        onDispose {
            Platform.runLater {
                engineState?.load(null)
            }
        }
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            JFXPanel().also { jfxPanel ->
                // 登录页切换登录方式时会移除这个面板。JavaFX 默认在最后一个面板移除后退出，之后新建的 WebView 不再加载页面。
                Platform.setImplicitExit(false)
                jfxPanel.enableInputMethods(false)
                Platform.runLater {
                    WebViewCookies.startSession(initialCookies)
                    val webView = WebView().apply {
                        engine.userAgent = userAgent
                        isContextMenuEnabled = true
                    }
                    val engine = webView.engine
                    engineState = engine

                    // 监听页面加载完成，与 Android 端 WebViewClient.onPageFinished 对应
                    engine.loadWorker.stateProperty().addListener { _, _, newState ->
                        if (newState == Worker.State.SUCCEEDED) {
                            currentOnPageFinished(engine.location)
                            currentOnCookiesChanged(WebViewCookies.zhihuCookies())
                        }
                    }

                    jfxPanel.scene = Scene(webView)

                    // 加载目标 URL
                    if (url != null) {
                        engine.load(url)
                    }
                }
            }
        },
    )
}

/**
 * 桌面端风控验证 WebView 容器。
 *
 * 对应共享登录页中的二维码风控内容区域，
 * 负责加载风控验证 URL 并在用户完成验证后回传 cookies。
 */
@Composable
fun DesktopRiskControlWebView(
    url: String,
    cookies: Map<String, String>,
    onCookiesChanged: (Map<String, String>) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        DesktopWebviewComp(
            url = url,
            modifier = Modifier.fillMaxSize(),
            initialCookies = cookies,
            onPageFinished = {},
            onCookiesChanged = { updatedCookies ->
                onCookiesChanged(updatedCookies)
            },
        )
    }
}

/**
 * 内嵌 WebView 共用的 cookie 存储，内部是 JavaFX 默认使用的 [CookieManager]，cookie 的处理与默认行为一致。
 *
 * JavaFX 的 HTTP/2 加载器在类初始化时就固定了当时的 [CookieHandler.getDefault]，所以必须在第一个 WebView 加载页面前
 * 装上它，换会话时只能换掉内部的实例。登录态也要从这里读：页面里的 document.cookie 读不到 HttpOnly 的 z_c0。
 */
internal object WebViewCookies : CookieHandler() {
    private val zhihu = URI(ZHIHU_HOME_URL)

    @Volatile
    private var store = CookieManager()

    /** 让接下来打开的页面只带着 [cookies]。 */
    fun startSession(cookies: Map<String, String>) {
        if (getDefault() !== this) setDefault(this)
        store = CookieManager()
        if (cookies.isNotEmpty()) {
            put(zhihu, mapOf("Set-Cookie" to cookies.map { (name, value) -> "$name=$value; Domain=.zhihu.com; Path=/" }))
        }
    }

    fun zhihuCookies(): Map<String, String> =
        parseCookieAssignments(get(zhihu, emptyMap())["Cookie"].orEmpty().joinToString("; "))

    override fun get(uri: URI, requestHeaders: Map<String, List<String>>): Map<String, List<String>> =
        store.get(uri, requestHeaders)

    override fun put(uri: URI, responseHeaders: Map<String, List<String>>) = store.put(uri, responseHeaders)
}
