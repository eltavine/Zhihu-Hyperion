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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.platform.presentShareSheet
import com.github.zly2006.zhihu.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.platform.rememberUserMessageSink

/** iOS 的“分享”打开系统分享面板；“复制链接”仍直接复制。 */
@Composable
actual fun rememberShareActionExecutor(): ShareActionExecutor {
    val copyPlainText = rememberPlainTextClipboard()
    val userMessages = rememberUserMessageSink()
    return remember(copyPlainText, userMessages) {
        val copyLink = clipboardShareActionExecutor(copyPlainText, userMessages)
        object : ShareActionExecutor {
            override fun invoke(action: ShareAction, content: NavDestination, shareText: String) {
                if (action == ShareAction.CopyLink) {
                    copyLink(action, content, shareText)
                } else {
                    presentShareSheet(listOf(shareText))
                }
            }
        }
    }
}
