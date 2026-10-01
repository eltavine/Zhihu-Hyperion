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

package com.github.zly2006.zhihu.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.github.zly2006.zhihu.nativeZhihuModules
import com.github.zly2006.zhihu.platform.NativeUserMessageHost
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.NativeZhihuMain
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook

/** SwiftUI `App` 初始化时调用，每个进程一次；必须在创建 [mainViewController] 之前完成。 */
@OptIn(ExperimentalNativeApi::class)
fun startZhihuApp() {
    setUnhandledExceptionHook { error ->
        runCatching {
            println("Unhandled coroutine exception: ${error.message ?: error::class.simpleName}")
            error.printStackTrace()
        }
    }
    startKoin { modules(nativeZhihuModules()) }
}

/** SwiftUI 场景承载的共享 Compose 界面。 */
fun mainViewController(): UIViewController = ComposeUIViewController {
    ZhihuTheme {
        NativeUserMessageHost {
            NativeZhihuMain()
        }
    }
}
