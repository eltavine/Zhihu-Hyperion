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

import com.github.zly2006.zhihu.util.TextDocumentStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

private val customAiJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** 自定义 AI 的连接配置；与普通设置分开存放，不随 WebDAV 同步，也排除在 Android 云备份之外。 */
@Serializable
data class CustomAiConfig(
    /** OpenAI 兼容接口的根地址（如 `https://api.deepseek.com/v1`），也可以直接填到 `/chat/completions`。 */
    val endpoint: String = "",
    /** 本地模型服务（如 Ollama）不校验密钥，可以留空。 */
    val apiKey: String = "",
    val model: String = "",
    /** 为 true 时「总结本文」改用这里的模型，而不是知乎直答。 */
    val useForSummary: Boolean = false,
) {
    val isComplete: Boolean
        get() = endpoint.isNotBlank() && model.isNotBlank()
}

class CustomAiConfigFile(
    private val document: TextDocumentStore,
) {
    fun load(): CustomAiConfig = document
        .readText()
        ?.let { runCatching { customAiJson.decodeFromString(CustomAiConfig.serializer(), it) }.getOrNull() }
        ?: CustomAiConfig()

    fun save(config: CustomAiConfig) = document.writeText(customAiJson.encodeToString(CustomAiConfig.serializer(), config))
}

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
)

/** 流式分片和非流式完整结果共用：前者把文字放在 `delta`，后者放在 `message`。 */
@Serializable
private data class ChatCompletionChunk(
    val choices: List<Choice> = emptyList(),
) {
    @Serializable
    data class Choice(
        val delta: Content? = null,
        val message: Content? = null,
    )

    @Serializable
    data class Content(
        val content: String? = null,
    )
}

internal fun chatCompletionsUrl(endpoint: String): String {
    val trimmed = endpoint.trim().trimEnd('/')
    return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
}

/**
 * 调用 OpenAI 兼容的 Chat Completions 接口，逐段返回模型生成的文字。
 *
 * DeepSeek、通义千问、Kimi、智谱、OpenRouter 和本地 Ollama 都提供这一协议；服务不支持流式、直接回完整 JSON 时也能读出结果。
 * 请求用 [engine] 新建客户端，不带知乎账号的 Cookie。
 */
fun streamChatCompletion(
    engine: HttpClientEngine,
    config: CustomAiConfig,
    messages: List<ChatMessage>,
): Flow<String> = channelFlow {
    // 推理模型在输出正文前可能沉默很久，读超时放宽到一分钟。
    HttpClient(engine) { install(HttpTimeout) { socketTimeoutMillis = 60_000 } }.use { client ->
        client
            .preparePost(chatCompletionsUrl(config.endpoint)) {
                if (config.apiKey.isNotBlank()) bearerAuth(config.apiKey.trim())
                accept(ContentType.Text.EventStream)
                val request = ChatCompletionRequest(model = config.model.trim(), messages = messages)
                setBody(TextContent(customAiJson.encodeToString(ChatCompletionRequest.serializer(), request), ContentType.Application.Json))
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val body = response.bodyAsText()
                    error(chatErrorMessage(body) ?: "请求失败（HTTP ${response.status.value}）")
                }
                if (response.contentType()?.match(ContentType.Application.Json) == true) {
                    chatCompletionText(response.bodyAsText(), streamed = false)?.let { send(it) }
                    return@execute
                }
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    chatCompletionText(data, streamed = true)?.let { send(it) }
                }
            }
    }
}

/** 解析一个流式分片或一份完整结果里的文字；服务在响应体或流中返回错误时抛出带原因的异常。 */
internal fun chatCompletionText(
    payload: String,
    streamed: Boolean,
): String? {
    val element = customAiJson.parseToJsonElement(payload).jsonObject
    chatErrorMessage(element)?.let { error(it) }
    val choice = customAiJson.decodeFromJsonElement<ChatCompletionChunk>(element).choices.firstOrNull() ?: return null
    return (if (streamed) choice.delta else choice.message)?.content?.takeIf { it.isNotEmpty() }
}

/** 兼容各家服务的错误格式：`{"error": {"message": ...}}`、`{"error": "..."}`、`{"message": ...}` 或 `{"detail": ...}`。 */
internal fun chatErrorMessage(body: String): String? = runCatching { customAiJson.parseToJsonElement(body).jsonObject }
    .getOrNull()
    ?.let(::chatErrorMessage)

private fun chatErrorMessage(element: JsonObject): String? {
    val error = element["error"]
    val message = when {
        error is JsonPrimitive -> error.contentOrNull
        error is JsonObject -> (error["message"] as? JsonPrimitive)?.contentOrNull
        "choices" in element -> null
        else -> (element["message"] as? JsonPrimitive)?.contentOrNull ?: (element["detail"] as? JsonPrimitive)?.contentOrNull
    }
    return message?.takeIf { it.isNotBlank() }
}
