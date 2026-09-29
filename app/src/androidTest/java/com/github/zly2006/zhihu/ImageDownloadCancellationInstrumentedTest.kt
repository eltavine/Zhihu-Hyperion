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

package com.github.zly2006.zhihu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class ImageDownloadCancellationInstrumentedTest {
    /**
     * Regression: https://github.com/eltavine/Zhihu-Hyperion/actions/runs/36450667729
     * Fixed by: https://github.com/eltavine/Zhihu-Hyperion/pull/2
     *
     * 列表滑走或页面销毁时，Compose 会在主线程取消仍在下载的 AsyncImage 请求；取消过程不能产生未捕获的协程异常，
     * 否则生产上会交给线程默认处理器直接结束进程。这里经由 Coil 默认网络抓取器请求一个发完首块数据就停住的本地服务器，
     * 等响应体读取停在 socket 上后，在主线程取消下载。
     */
    @Test
    fun cancellingBlockedImageDownloadRaisesNoUncaughtException() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Android's getLoopbackAddress() is ::1, which a request to 127.0.0.1 cannot reach.
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val releaseServer = CountDownLatch(1)
            thread(name = "stalled-image-server", isDaemon = true) {
                val socket = try {
                    server.accept()
                } catch (_: SocketException) {
                    return@thread
                }
                socket.use {
                    val request = it.getInputStream().bufferedReader()
                    while (!request.readLine().isNullOrEmpty()) Unit
                    it.getOutputStream().run {
                        write("HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\nContent-Length: 1048576\r\n\r\n".toByteArray())
                        write(ByteArray(1024))
                        flush()
                    }
                    releaseServer.await()
                }
            }
            val uncaught = AtomicReference<Throwable?>()
            val imageLoader = ImageLoader
                .Builder(context)
                .memoryCache(null)
                .diskCache(null)
                .build()
            var result: ImageResult? = null
            val download = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e -> uncaught.set(e) }).launch {
                result = imageLoader.execute(ImageRequest.Builder(context).data("http://127.0.0.1:${server.localPort}/stalled.jpg").build())
            }
            // Cancelling before the body read starts would leave no response stream to close.
            withTimeout(10_000) {
                while (
                    Thread.getAllStackTraces().values.none { stack ->
                        stack.any { it.className.endsWith("SocketInputStream") } &&
                            stack.any { it.className.startsWith("com.android.okhttp.") || it.className.startsWith("okhttp3.") }
                    }
                ) {
                    check(download.isActive) { "The download ended before its body read blocked: ${(result as? ErrorResult)?.throwable}" }
                    delay(20)
                }
            }
            withContext(Dispatchers.Main) { download.cancel() }
            download.join()
            releaseServer.countDown()
            imageLoader.shutdown()

            assertNull(uncaught.get()?.stackTraceToString(), uncaught.get())
        }
    }
}
