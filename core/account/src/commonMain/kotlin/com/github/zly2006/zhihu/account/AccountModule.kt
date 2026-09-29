/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

import com.github.zly2006.zhihu.util.AtomicTextFile
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.engine.HttpClientEngine
import kotlinx.io.files.Path
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * 进程级账户所有权。组合根传入平台账户文件路径；所有账户相关的 HttpClient 共享同一个引擎，
 * 测试只需覆盖 [HttpClientEngine] 与 [ZhihuAccountStore] 两个绑定。
 */
fun accountModule(accountFile: Path) = module {
    single<HttpClientEngine> { createAccountHttpClientEngine() } onClose { it?.close() }
    single { ZhihuAccountRepository(AtomicTextFile(accountFile)) }
    single { ZhihuAccountStore(get(), get()) } onClose { it?.close() }
    single<ZhihuApiEnvironment> { ZhihuAccountApiEnvironment(get()) }
}
