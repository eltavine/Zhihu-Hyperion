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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon
import com.github.zly2006.zhihu.platform.rememberExternalUrlOpener
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
    val digitsFocusRequester = remember { FocusRequester() }
    // 未勾选协议时按钮照常可点，点击后先弹窗征求同意，同意后接着执行这次操作。
    var pendingAgreementAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val afterAgreement: (() -> Unit) -> Unit = { action ->
        if (agreementAccepted) action() else pendingAgreementAction = action
    }
    val openExternalUrl = rememberExternalUrlOpener()
    val linkStyles = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary))
    val agreementDocuments = buildAnnotatedString {
        for ((title, url) in listOf(
            "《知乎协议》" to "https://www.zhihu.com/term/zhihu-terms",
            "《个人信息保护指引》" to "https://www.zhihu.com/term/privacy",
        )) {
            withLink(LinkAnnotation.Url(url, linkStyles) { openExternalUrl(url) }) { append(title) }
        }
    }

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
                    // 聚焦后键盘直接切到验证码框，iOS 键盘上方也会出现“来自信息”的验证码。
                    digitsFocusRequester.requestFocus()
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
    val canSignIn = hasRequestedDigits &&
        phoneNumber.length == 11 &&
        digits.length == 6 &&
        !isSendingDigits &&
        !isLoggingIn
    val signIn: () -> Unit = {
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canSignIn) afterAgreement(signIn) else defaultKeyboardAction(ImeAction.Done)
                    },
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(digitsFocusRequester)
                    .semantics { contentType = ContentType.SmsOtpCode }
                    .testTag("phone_login_digits"),
            )
            // 隐形的占位按钮撑住最宽文案的宽度，倒计时和“重新发送”切换时按钮不缩放，验证码框也不横向跳动。
            Box {
                for (widestLabel in listOf("发送验证码", "60 秒后重发")) {
                    MediumActionButton(
                        text = widestLabel,
                        onClick = {},
                        emphasis = ActionEmphasis.Tonal,
                        enabled = false,
                        modifier = Modifier
                            .alpha(0f)
                            .clearAndSetSemantics {},
                    )
                }
                MediumActionButton(
                    text = when {
                        resendSeconds > 0 -> "$resendSeconds 秒后重发"
                        hasRequestedDigits -> "重新发送"
                        else -> "发送验证码"
                    },
                    onClick = { afterAgreement { scope.launch { sendDigits() } } },
                    emphasis = ActionEmphasis.Tonal,
                    enabled = phoneNumber.length == 11 && resendSeconds == 0 && !isLoggingIn,
                    loading = isSendingDigits,
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("phone_login_send_digits"),
                )
            }
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
                text = buildAnnotatedString {
                    append("我已阅读并同意")
                    append(agreementDocuments)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }

        MediumActionButton(
            text = "登录",
            onClick = { afterAgreement(signIn) },
            enabled = canSignIn,
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

    pendingAgreementAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAgreementAction = null },
            icon = { Icon(AppIcons.License, contentDescription = null) },
            title = { Text("请先阅读并同意") },
            text = {
                Text(
                    buildAnnotatedString {
                        append("登录前需要阅读并同意")
                        append(agreementDocuments)
                        append("。")
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        agreementAccepted = true
                        pendingAgreementAction = null
                        action()
                    },
                    modifier = Modifier.testTag("phone_login_agreement_accept"),
                ) {
                    Text("同意并继续")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAgreementAction = null }) {
                    Text("取消")
                }
            },
        )
    }
}
