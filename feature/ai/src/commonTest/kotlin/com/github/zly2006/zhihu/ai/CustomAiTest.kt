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
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomAiTest {
    private val config = CustomAiConfig(endpoint = "https://api.example.com/v1/", apiKey = " sk-test ", model = " demo-model ")

    private fun mockEngine(
        status: HttpStatusCode = HttpStatusCode.OK,
        contentType: String = "text/event-stream",
        body: String,
        onRequest: (HttpRequestData) -> Unit = {},
    ) = MockEngine { request ->
        onRequest(request)
        respond(body, status, headersOf(HttpHeaders.ContentType, contentType))
    }

    @Test
    fun streamsDeltasUntilDoneAndSendsOpenAiRequest() = runTest {
        var captured: HttpRequestData? = null
        val stream =
            """
            : keep-alive

            data: {"choices":[{"delta":{"role":"assistant"}}]}

            data: {"choices":[{"delta":{"content":"第一段"}}]}

            data: {"choices":[{"delta":{"reasoning_content":"思考中"}}]}

            data: {"choices":[{"delta":{"content":"，第二段"}}]}

            data: [DONE]

            data: {"choices":[{"delta":{"content":"不应读取"}}]}
            """.trimIndent()

        val chunks = streamChatCompletion(
            mockEngine(body = stream) { captured = it },
            config,
            listOf(ChatMessage("system", "总结"), ChatMessage("user", "正文")),
        ).toList()

        assertEquals(listOf("第一段", "，第二段"), chunks)
        val request = checkNotNull(captured)
        assertEquals("https://api.example.com/v1/chat/completions", request.url.toString())
        assertEquals("Bearer sk-test", request.headers[HttpHeaders.Authorization])
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals("demo-model", body["model"]?.jsonPrimitive?.content)
        assertTrue(body["stream"]!!.jsonPrimitive.boolean)
        assertEquals(
            "user",
            body["messages"]!!
                .jsonArray[1]
                .jsonObject["role"]
                ?.jsonPrimitive
                ?.content,
        )
    }

    @Test
    fun keepsFullEndpointAndSkipsAuthorizationWithoutKey() = runTest {
        var captured: HttpRequestData? = null
        streamChatCompletion(
            mockEngine(body = "data: [DONE]") { captured = it },
            CustomAiConfig(endpoint = "http://localhost:11434/v1/chat/completions", model = "qwen3"),
            listOf(ChatMessage("user", "hi")),
        ).toList()

        val request = checkNotNull(captured)
        assertEquals("http://localhost:11434/v1/chat/completions", request.url.toString())
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun readsWholeJsonWhenServerDoesNotStream() = runTest {
        val chunks = streamChatCompletion(
            mockEngine(contentType = "application/json", body = """{"choices":[{"message":{"content":"完整结果"}}]}"""),
            config,
            listOf(ChatMessage("user", "hi")),
        ).toList()

        assertEquals(listOf("完整结果"), chunks)
    }

    @Test
    fun surfacesProviderErrorMessages() = runTest {
        val httpError = assertFailsWith<IllegalStateException> {
            streamChatCompletion(
                mockEngine(
                    status = HttpStatusCode.Unauthorized,
                    contentType = "application/json",
                    body = """{"error":{"message":"Incorrect API key provided","type":"invalid_request_error"}}""",
                ),
                config,
                listOf(ChatMessage("user", "hi")),
            ).toList()
        }
        assertEquals("Incorrect API key provided", httpError.message)

        val streamError = assertFailsWith<IllegalStateException> {
            streamChatCompletion(
                mockEngine(body = "data: {\"error\":\"model not found\"}\n\n"),
                config,
                listOf(ChatMessage("user", "hi")),
            ).toList()
        }
        assertEquals("model not found", streamError.message)
    }

    @Test
    fun configFileRoundTripsAndToleratesCorruptedContent() {
        val document = object : TextDocumentStore {
            var text: String? = null

            override fun readText(): String? = text

            override fun writeText(text: String) {
                this.text = text
            }

            override fun delete() {
                text = null
            }
        }
        val file = CustomAiConfigFile(document)
        assertEquals(CustomAiConfig(), file.load())

        val saved = CustomAiConfig(endpoint = "https://api.deepseek.com/v1", apiKey = "sk-test", model = "deepseek-chat", useForSummary = true)
        file.save(saved)
        assertEquals(saved, file.load())
        assertTrue(saved.isComplete)
        assertFalse(saved.copy(model = " ").isComplete)

        document.text = "{not json"
        assertEquals(CustomAiConfig(), file.load())
    }

    @Test
    fun fallsBackToStatusWhenErrorBodyIsNotJson() = runTest {
        val error = assertFailsWith<IllegalStateException> {
            streamChatCompletion(
                mockEngine(status = HttpStatusCode.BadGateway, contentType = "text/html", body = "<html>Bad Gateway</html>"),
                config,
                listOf(ChatMessage("user", "hi")),
            ).toList()
        }
        assertEquals("请求失败（HTTP 502）", error.message)
        assertEquals("upstream", chatErrorMessage("""{"detail":"upstream"}"""))
    }
}
