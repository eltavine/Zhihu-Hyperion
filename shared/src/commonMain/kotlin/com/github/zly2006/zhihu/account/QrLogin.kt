/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
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

package com.github.zly2006.zhihu.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SharedQrLoginPane(
    onLoginSuccess: suspend (Map<String, String>) -> Boolean,
    modifier: Modifier = Modifier,
    generateQrBitmap: (String) -> ImageBitmap = ::generateQrLoginBitmap,
    initialCookies: Map<String, String> = emptyMap(),
    qrReadyMessage: String = "请打开知乎++ App 扫一扫",
    onQrReady: () -> Unit = {},
) {
    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var statusText by remember { mutableStateOf("正在获取二维码") }
    var sessionCookies by remember { mutableStateOf(initialCookies.toMap()) }
    var riskControlUrl by remember { mutableStateOf<String?>(null) }
    var riskControlMessage by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(true) }
    val cookies = remember(refreshKey) { sessionCookies.toMutableMap() }
    val client = rememberLoginHttpClient(cookies)

    LaunchedEffect(refreshKey) {
        qrBitmap = null
        statusText = "正在获取二维码"
        riskControlUrl = null
        isWorking = true

        try {
            withTimeoutOrNull(QR_LOGIN_REQUEST_TIMEOUT) {
                prefetchQrLoginContext(client, cookies)
            } ?: throw IllegalStateException("二维码登录初始化超时，请重试")
            val qrCode = withTimeoutOrNull(QR_LOGIN_REQUEST_TIMEOUT) {
                requestQrCode(client, cookies)
            } ?: throw IllegalStateException("二维码获取超时，请重试")
            sessionCookies = cookies.toMap()
            val qrLink = qrCode.link ?: throw IllegalStateException("知乎没有返回二维码链接")
            val qrToken = qrCode.token ?: qrCode.qrcodeToken ?: throw IllegalStateException("知乎没有返回二维码 token")
            qrBitmap = generateQrBitmap(qrLink)
            statusText = qrReadyMessage
            onQrReady()

            val success = pollQrCodeLogin(
                client = client,
                cookies = cookies,
                token = qrToken,
                deadline = normalizeDeadline(qrCode.expiresAt),
                onScanned = {
                    statusText = "请在知乎 App 上确认登录"
                },
                onRiskControl = { message, redirectUrl ->
                    sessionCookies = cookies.toMap()
                    riskControlMessage = message ?: "知乎需要验证当前网络环境"
                    riskControlUrl = redirectUrl ?: ZHIHU_RISK_CONTROL_URL
                    statusText = riskControlMessage ?: "知乎需要验证当前网络环境"
                },
            )

            if (success) {
                statusText = "正在验证登录"
                isWorking = false
                statusText = if (onLoginSuccess(cookies)) {
                    "登录成功"
                } else {
                    "登录结果验证失败，请重试"
                }
            } else if (!riskControlUrl.isNullOrBlank()) {
                isWorking = false
            } else {
                statusText = "二维码已过期，请重试"
                isWorking = false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            statusText = e.message ?: "二维码获取失败，请重试"
            isWorking = false
        }
    }

    val currentRiskControlUrl = riskControlUrl
    if (!currentRiskControlUrl.isNullOrBlank()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("qr_risk_control_content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = riskControlMessage ?: "请先完成知乎的网络环境验证",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    riskControlUrl = null
                    riskControlMessage = null
                    refreshKey += 1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_risk_control_continue"),
            ) {
                Text("完成验证后继续扫码")
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (isLoginRiskControlSupported) {
                    LoginRiskControlPane(
                        url = currentRiskControlUrl,
                        cookies = sessionCookies,
                        onCookiesChanged = { updatedCookies ->
                            sessionCookies = sessionCookies + updatedCookies
                        },
                    )
                } else {
                    Text(
                        text = "当前被知乎风控，请过几个小时再试",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_login_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap!!,
                contentDescription = "知乎登录二维码",
                modifier = Modifier
                    .size(260.dp)
                    .testTag("qr_login_image"),
            )
            Spacer(modifier = Modifier.size(16.dp))
        } else if (isWorking) {
            CircularProgressIndicator(
                modifier = Modifier.testTag("qr_login_loading"),
            )
            Spacer(modifier = Modifier.size(16.dp))
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("qr_login_status"),
        )

        if (!riskControlMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = riskControlMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.size(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    qrBitmap = null
                    statusText = "正在刷新二维码"
                    isWorking = true
                    riskControlUrl = null
                    riskControlMessage = null
                    refreshKey += 1
                },
                modifier = Modifier.testTag("qr_login_retry"),
            ) {
                Text("刷新二维码")
            }
        }
    }
}

expect fun generateQrLoginBitmap(content: String): ImageBitmap

expect val isLoginRiskControlSupported: Boolean

@Composable
expect fun LoginRiskControlPane(
    url: String,
    cookies: Map<String, String>,
    onCookiesChanged: (Map<String, String>) -> Unit,
)
