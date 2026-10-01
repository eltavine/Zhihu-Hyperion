/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon
import com.github.zly2006.zhihu.ui.components.ActionEmphasis
import com.github.zly2006.zhihu.ui.components.MediumActionButton
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
expect fun rememberLoginHttpClient(cookies: MutableMap<String, String>): HttpClient

expect fun decodePhoneLoginCaptchaImage(content: String): ImageBitmap?

internal expect val phoneLoginDeviceInfo: ZhihuPhoneLoginDeviceInfo

/** 短信验证码输入框的平台输入选项；iOS 标成一次性验证码，键盘上会出现“来自信息”的验证码，其他平台为 null。 */
internal expect val smsCodeImeOptions: PlatformImeOptions?

@Composable
fun PhoneLoginPane(onLoginSuccess: (String) -> Unit) {
    val accountStore = koinInject<ZhihuAccountStore>()
    val engine = koinInject<HttpClientEngine>()
    val loginClient = remember(engine) { ZhihuPhoneLoginClient(phoneLoginDeviceInfo, engine) }
    DisposableEffect(loginClient) {
        onDispose(loginClient::close)
    }

    val scope = rememberCoroutineScope()
    var phoneNumber by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("") }
    var captchaInput by remember { mutableStateOf("") }
    var captchaImageBase64 by remember { mutableStateOf<String?>(null) }
    var captchaRequired by remember { mutableStateOf(false) }
    var agreementAccepted by remember { mutableStateOf(false) }
    var hasRequestedDigits by remember { mutableStateOf(false) }
    var isSendingDigits by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var resendSeconds by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1_000)
            resendSeconds--
        }
    }

    val sendDigits: suspend () -> Unit = {
        errorMessage = null
        isSendingDigits = true
        try {
            when (val result = loginClient.requestDigits(phoneNumber)) {
                ZhihuPhoneDigitsResult.Sent -> {
                    hasRequestedDigits = true
                    captchaRequired = false
                    captchaImageBase64 = null
                    captchaInput = ""
                    resendSeconds = 60
                }

                is ZhihuPhoneDigitsResult.CaptchaRequired -> {
                    captchaRequired = true
                    captchaImageBase64 = result.imageBase64
                    if (result.imageBase64 == null) {
                        errorMessage = "服务器未返回图形验证码，请换一张重试"
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = error.message ?: "发送验证码失败"
        } finally {
            isSendingDigits = false
        }
    }
    val verifyCaptchaAndSend: suspend () -> Unit = {
        isSendingDigits = true
        errorMessage = null
        try {
            if (loginClient.verifyCaptcha(captchaInput)) {
                isSendingDigits = false
                sendDigits()
            } else {
                errorMessage = "图形验证码不正确"
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = error.message ?: "验证图形验证码失败"
        } finally {
            isSendingDigits = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "使用知乎官方 Android 登录协议。是否需要图形验证码由知乎风控实时决定。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { value ->
                val nextPhoneNumber = value.filter(Char::isDigit).take(11)
                if (phoneNumber != nextPhoneNumber) {
                    phoneNumber = nextPhoneNumber
                    digits = ""
                    hasRequestedDigits = false
                    captchaRequired = false
                    captchaImageBase64 = null
                    captchaInput = ""
                }
            },
            label = { Text("手机号") },
            leadingIcon = { Icon(AppIcons.Smartphone, contentDescription = null) },
            prefix = { Text("+86 ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.PhoneNumberNational }
                .testTag("phone_login_phone"),
        )

        if (captchaRequired) {
            val captchaBitmap = remember(captchaImageBase64) {
                captchaImageBase64?.let(::decodePhoneLoginCaptchaImage)
            }
            captchaBitmap?.let { image ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        bitmap = image,
                        contentDescription = "图形验证码",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(8.dp)
                            .height(96.dp)
                            .testTag("phone_login_captcha_image"),
                    )
                }
            }
            OutlinedTextField(
                value = captchaInput,
                onValueChange = { captchaInput = it },
                label = { Text("图形验证码") },
                leadingIcon = { Icon(AppIcons.Image, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (captchaInput.isNotBlank() && !isSendingDigits) {
                            scope.launch { verifyCaptchaAndSend() }
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_login_captcha_input"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MediumActionButton(
                    text = "换一张",
                    onClick = {
                        scope.launch {
                            isSendingDigits = true
                            errorMessage = null
                            try {
                                captchaImageBase64 = loginClient.refreshCaptcha()
                                if (captchaImageBase64 == null) {
                                    errorMessage = "服务器未返回图形验证码，请稍后重试"
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                errorMessage = error.message ?: "刷新图形验证码失败"
                            } finally {
                                isSendingDigits = false
                            }
                        }
                    },
                    emphasis = ActionEmphasis.Outlined,
                    icon = AppIcons.Refresh,
                    enabled = !isSendingDigits,
                    modifier = Modifier.weight(1f),
                )
                MediumActionButton(
                    text = "验证并发送",
                    onClick = { scope.launch { verifyCaptchaAndSend() } },
                    emphasis = ActionEmphasis.Tonal,
                    enabled = captchaInput.isNotBlank() && !isSendingDigits,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("phone_login_verify_captcha"),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            // 输入框顶部为浮动标签留了空白，底部对齐才能让按钮和输入框的外框等高并齐。
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = digits,
                onValueChange = { value ->
                    digits = value.filter(Char::isDigit).take(6)
                },
                label = { Text("短信验证码") },
                leadingIcon = { Icon(AppIcons.Sms, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    platformImeOptions = smsCodeImeOptions,
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentType = ContentType.SmsOtpCode }
                    .testTag("phone_login_digits"),
            )
            MediumActionButton(
                text = if (resendSeconds > 0) "${resendSeconds}s" else "发送验证码",
                onClick = { scope.launch { sendDigits() } },
                emphasis = ActionEmphasis.Tonal,
                enabled = agreementAccepted &&
                    phoneNumber.length == 11 &&
                    resendSeconds == 0 &&
                    !isLoggingIn,
                loading = isSendingDigits,
                modifier = Modifier.testTag("phone_login_send_digits"),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .toggleable(
                    value = agreementAccepted,
                    role = Role.Checkbox,
                    onValueChange = { agreementAccepted = it },
                ).padding(vertical = 4.dp)
                .testTag("phone_login_agreement"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = agreementAccepted,
                onCheckedChange = null,
                modifier = Modifier.padding(12.dp),
            )
            Text(
                text = "我已阅读并同意《知乎协议》《个人信息保护指引》",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }

        MediumActionButton(
            text = "登录",
            onClick = {
                scope.launch {
                    isLoggingIn = true
                    errorMessage = null
                    try {
                        val token = loginClient.signIn(phoneNumber, digits)
                        if (accountStore.login(token)) {
                            onLoginSuccess(accountStore.session.username)
                        } else {
                            errorMessage = "登录凭证验证失败，请重试"
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        errorMessage = error.message ?: "登录失败"
                    } finally {
                        isLoggingIn = false
                    }
                }
            },
            enabled = agreementAccepted &&
                hasRequestedDigits &&
                phoneNumber.length == 11 &&
                digits.length == 6 &&
                !isSendingDigits,
            loading = isLoggingIn,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_login_submit"),
        )

        errorMessage?.let { message ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_login_error"),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.Error, contentDescription = null)
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
