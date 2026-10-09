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

package com.github.zly2006.zhihu.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.icons.Icon
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

const val CUSTOM_AI_ENDPOINT_TAG = "customAi:endpoint"
const val CUSTOM_AI_API_KEY_TAG = "customAi:apiKey"
const val CUSTOM_AI_MODEL_TAG = "customAi:model"
const val CUSTOM_AI_TEST_TAG = "customAi:test"
const val CUSTOM_AI_SAVE_TAG = "customAi:save"

/**
 * 填写自定义 AI 的 OpenAI 兼容接口：地址、API Key 和模型，可以先测试连接再保存。
 *
 * 保存时只交出新的配置，持久化和后续请求由调用方决定。
 */
@Composable
fun CustomAiConfigDialog(
    initialConfig: CustomAiConfig,
    onDismissRequest: () -> Unit,
    onSave: (CustomAiConfig) -> Unit,
) {
    val engine = koinInject<HttpClientEngine>()
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(initialConfig) }
    var showApiKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testFailed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(AppIcons.SmartToy, contentDescription = null) },
        title = { Text("自定义 AI") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "填写 OpenAI 兼容接口，例如 DeepSeek、通义千问、Kimi、智谱、OpenRouter 或本地 Ollama。" +
                        "总结时会把标题和正文发送给这个服务；API Key 只保存在本机。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = config.endpoint,
                    onValueChange = { config = config.copy(endpoint = it) },
                    label = { Text("API 地址") },
                    placeholder = { Text("https://api.deepseek.com/v1") },
                    leadingIcon = { Icon(AppIcons.Dns, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(CUSTOM_AI_ENDPOINT_TAG),
                )
                OutlinedTextField(
                    value = config.apiKey,
                    onValueChange = { config = config.copy(apiKey = it) },
                    label = { Text("API Key") },
                    supportingText = { Text("本地服务不需要时可以留空") },
                    leadingIcon = { Icon(AppIcons.Password, contentDescription = null) },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                icon = if (showApiKey) AppIcons.Visibility else AppIcons.VisibilityOff,
                                contentDescription = if (showApiKey) "隐藏 API Key" else "显示 API Key",
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(CUSTOM_AI_API_KEY_TAG),
                )
                OutlinedTextField(
                    value = config.model,
                    onValueChange = { config = config.copy(model = it) },
                    label = { Text("模型") },
                    placeholder = { Text("deepseek-chat") },
                    leadingIcon = { Icon(AppIcons.Memory, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(CUSTOM_AI_MODEL_TAG),
                )
                TextButton(
                    onClick = {
                        testing = true
                        testResult = null
                        scope.launch {
                            try {
                                val reply = streamChatCompletion(engine, config, listOf(ChatMessage("user", "请只回复：连接成功")))
                                    .fold("") { text, chunk -> text + chunk }
                                testFailed = false
                                testResult = if (reply.isBlank()) "连接成功，但模型没有返回文字" else "连接成功：${reply.trim().take(60)}"
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                testFailed = true
                                testResult = "连接失败：${e.message ?: e::class.simpleName}"
                            } finally {
                                testing = false
                            }
                        }
                    },
                    enabled = config.isComplete && !testing,
                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    modifier = Modifier.testTag(CUSTOM_AI_TEST_TAG),
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                    } else {
                        Icon(AppIcons.NetworkCheck, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    }
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("测试连接")
                }
                testResult?.let { result ->
                    Text(
                        result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(config.copy(endpoint = config.endpoint.trim(), apiKey = config.apiKey.trim(), model = config.model.trim()))
                },
                modifier = Modifier.testTag(CUSTOM_AI_SAVE_TAG),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("取消") }
        },
    )
}
