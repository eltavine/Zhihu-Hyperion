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
import androidx.startup.Initializer
import com.github.zly2006.zhihu.nlp.KeywordWeightExtractor
import com.github.zly2006.zhihu.nlp.NLPService
import com.github.zly2006.zhihu.nlp.NlpServiceKeywordSemanticMatcher
import com.github.zly2006.zhihu.viewmodel.filter.KeywordSemanticMatcher
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** lite 与 full 变体各自的 NLPService 决定语义屏蔽与关键词权重的实际能力。 */
private val nlpModule = module {
    single<KeywordSemanticMatcher> { NlpServiceKeywordSemanticMatcher }
    single { KeywordWeightExtractor { text, topN -> NLPService.extractKeywordsWithWeight(text, topN) } }
}

/**
 * Android 组合根。androidx.startup 的 ContentProvider 早于 Instrumentation.onCreate 与 Application.onCreate 执行，
 * 测试 runner 在 onCreate 阶段就会替换账户绑定，因此 Koin 不能等到 Application.onCreate 才启动。
 * Koin 自带的 KoinStartup 在 4.2.2 仍标注为 @KoinExperimentalAPI，这里只使用稳定的 startKoin。
 */
class ZhihuKoinInitializer : Initializer<Koin> {
    override fun create(context: Context): Koin = startKoin {
        androidContext(context)
        modules(androidZhihuModules(context) + nlpModule)
    }.koin

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
