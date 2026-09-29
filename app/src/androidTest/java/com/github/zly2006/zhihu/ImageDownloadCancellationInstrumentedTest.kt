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
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class ImageDownloadCancellationInstrumentedTest {
    /**
     * Regression: https://github.com/eltavine/Zhihu-Hyperion/actions/runs/36450667729
     * Fixed by: https://github.com/eltavine/Zhihu-Hyperion/pull/2
     *
     * 图片下载在响应体读取阻塞时被取消（滚动离开或页面销毁），取消过程不能产生未捕获的协程异常：
     * 生产上这类异常会交给线程默认处理器直接结束进程。这里经由 Coil 默认网络抓取器请求一个发完首块数据
     * 就停住的本地服务器，读取线程停在 socket 读上后再取消。
     */
    @Test
    fun cancellingBlockedImageDownloadRaisesNoUncaughtException() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val releaseServer = CountDownLatch(1)
            thread(name = "stalled-image-server") {
                server.accept().use { socket ->
                    val request = socket.getInputStream().bufferedReader()
                    while (!request.readLine().isNullOrEmpty()) Unit
                    socket.getOutputStream().run {
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
            val download = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e -> uncaught.set(e) }).launch {
                imageLoader.execute(ImageRequest.Builder(context).data("http://127.0.0.1:${server.localPort}/stalled.jpg").build())
            }
            // Cancelling before a reader is parked in the socket read would only close an idle stream, which never raced.
            withTimeout(10_000) {
                while (
                    Thread.getAllStackTraces().values.none { stack ->
                        stack.any { it.className == "java.net.SocketInputStream" } &&
                            stack.any { it.className.startsWith("com.android.okhttp.") || it.className.startsWith("okhttp3.") }
                    }
                ) {
                    delay(20)
                }
            }
            download.cancelAndJoin()
            releaseServer.countDown()
            imageLoader.shutdown()

            assertNull(uncaught.get()?.stackTraceToString(), uncaught.get())
        }
    }
}
