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

package com.github.zly2006.zhihu.ui.components

import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.navigation.AndroidArticleNavigationHandoff
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.platform.androidUserMessageSink
import com.github.zly2006.zhihu.util.clipboardManager
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform

actual suspend fun extractFeedKeywords(
    title: String,
    excerpt: String?,
): List<KeywordWithWeight> = KeywordAnalyzerCore.extractFromFeedWithWeight(
    title = title,
    excerpt = excerpt,
    content = null,
    topN = 10,
    extractor = KoinPlatform.getKoin().get(),
)

@Composable
actual fun rememberShareActionExecutor(): ShareActionExecutor {
    val context = LocalContext.current
    val articleNavigationHandoff = koinInject<AndroidArticleNavigationHandoff>()
    return remember(context) {
        object : ShareActionExecutor {
            override fun invoke(action: ShareAction, content: NavDestination, shareText: String) {
                if (action == ShareAction.CopyLink) {
                    articleNavigationHandoff.markClipboardDestination(content)
                    context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", shareText))
                    androidUserMessageSink(context).showShortMessage("已复制链接")
                } else {
                    val shareIntent = Intent().apply {
                        this.action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        if (action == ShareAction.DirectShare) {
                            putExtra(Intent.EXTRA_TITLE, getShareTitle(content))
                        }
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "分享到").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
    }
}
