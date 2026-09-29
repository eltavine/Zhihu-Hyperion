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

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

/**
 * Android uses OkHttp instead of Ktor's HttpURLConnection-based `Android` engine. Since Ktor 3.5.1
 * (https://youtrack.jetbrains.com/issue/KTOR-9629) cancelling a response body closes its `HttpURLConnection` stream on
 * the cancelling thread, and closing drains the stream: it throws `NetworkOnMainThreadException` when Compose cancels a
 * disposed image on the main thread, or `IllegalStateException: Unbalanced enter/exit` while another thread is blocked
 * reading. Either escapes as an uncaught coroutine exception and kills the app
 * (https://github.com/eltavine/Zhihu-Hyperion/actions/runs/36450667729). OkHttp cancels the call instead.
 * Coil's default network fetcher and the other `HttpClient()` callers pick the engine on the classpath, so no Android
 * module may depend on `ktor-client-android`.
 */
internal actual fun createAccountHttpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        // The Android engine's 100 s defaults; OkHttp's 10 s could cut off the AI summary event stream while the model
        // has not produced its first token.
        connectTimeout(100, TimeUnit.SECONDS)
        readTimeout(100, TimeUnit.SECONDS)
    }
}
