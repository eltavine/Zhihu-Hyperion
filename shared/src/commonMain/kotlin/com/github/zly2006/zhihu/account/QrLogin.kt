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

package com.github.zly2006.zhihu.account

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon
import com.github.zly2006.zhihu.ui.components.ActionEmphasis
import com.github.zly2006.zhihu.ui.components.AppLoadingIndicator
import com.github.zly2006.zhihu.ui.components.MediumActionButton
import com.github.zly2006.zhihu.ui.components.ShapedIcon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** [onUsePhoneLogin]：知乎要求的安全验证在本平台无法完成时，引导改用手机号登录。 */
@Composable
fun SharedQrLoginPane(
    onLoginSuccess: suspend (Map<String, String>) -> Boolean,
    onUsePhoneLogin: () -> Unit,
    modifier: Modifier = Modifier,
    initialCookies: Map<String, String> = emptyMap(),
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
            qrBitmap = generateQrLoginBitmap(qrLink)
            statusText = "打开知乎 App 扫一扫登录"

            val success = pollQrCodeLogin(
                client = client,
                cookies = cookies,
                token = qrToken,
                deadline = normalizeDeadline(qrCode.expiresAt),
                onScanned = {
                    statusText = "已扫码，请在手机上确认登录"
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

    val restartQrLogin = {
        qrBitmap = null
        statusText = "正在刷新二维码"
        isWorking = true
        riskControlUrl = null
        riskControlMessage = null
        refreshKey += 1
    }
    val currentRiskControlUrl = riskControlUrl
    if (!currentRiskControlUrl.isNullOrBlank() && isLoginRiskControlSupported) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
                .testTag("qr_risk_control_content"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.Error, contentDescription = null)
                    Text(
                        text = riskControlMessage ?: "请先完成知乎的网络环境验证",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large),
            ) {
                LoginRiskControlPane(
                    url = currentRiskControlUrl,
                    cookies = sessionCookies,
                    onCookiesChanged = { updatedCookies ->
                        sessionCookies = sessionCookies + updatedCookies
                    },
                )
            }
            MediumActionButton(
                text = "完成验证后继续扫码",
                onClick = restartQrLogin,
                emphasis = ActionEmphasis.Tonal,
                icon = AppIcons.Refresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_risk_control_continue"),
            )
        }
        return
    }
    if (!currentRiskControlUrl.isNullOrBlank()) {
        // 知乎给出的提示（“请点击下方验证按钮”）指向本平台没有的验证页，这里不转述它。
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("qr_risk_control_content"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ShapedIcon(AppIcons.Lock, containerColor = MaterialTheme.colorScheme.errorContainer)
                Text(
                    "暂时无法扫码登录",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "知乎要求当前网络先完成安全验证，这一步在本设备上无法完成。可以改用手机号登录，或过一段时间再试。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                MediumActionButton(
                    text = "改用手机号登录",
                    onClick = onUsePhoneLogin,
                    icon = AppIcons.Smartphone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_risk_control_use_phone"),
                )
                MediumActionButton(
                    text = "重试",
                    onClick = restartQrLogin,
                    emphasis = ActionEmphasis.Outlined,
                    icon = AppIcons.Refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_risk_control_retry"),
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_login_content"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 扫码要求深色码点印在浅色底上，深色主题下二维码也放在白底卡片里；卡片内的颜色因此按浅色底选：
            // 深色配色的 primary 是浅色调，inversePrimary 才是给浅色底用的深色调。
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val bitmap = qrBitmap
                    when {
                        bitmap != null -> Image(
                            bitmap = bitmap,
                            contentDescription = "知乎登录二维码",
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("qr_login_image"),
                        )

                        isWorking -> AppLoadingIndicator(
                            modifier = Modifier.testTag("qr_login_loading"),
                            color = MaterialTheme.colorScheme.run {
                                if (surface.luminance() < 0.5f) inversePrimary else primary
                            },
                        )

                        else -> Icon(
                            AppIcons.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Black.copy(alpha = 0.38f),
                        )
                    }
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("qr_login_status"),
            )

            riskControlMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            FilledTonalButton(
                onClick = restartQrLogin,
                modifier = Modifier.testTag("qr_login_retry"),
            ) {
                Icon(AppIcons.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
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
