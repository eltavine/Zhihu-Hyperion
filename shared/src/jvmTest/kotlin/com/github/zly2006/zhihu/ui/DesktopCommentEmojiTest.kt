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

package com.github.zly2006.zhihu.ui

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DesktopCommentEmojiTest {
    /** 安装后的 MSI、AppImage 从任意工作目录启动，评论表情不能依赖源码树里的 misc/。 */
    @Test
    fun commentEmojiResolvesOutsideTheSourceTree() {
        val workingDirectory = System.getProperty("user.dir")
        System.setProperty("user.dir", createTempDirectory().toString())
        try {
            assertEquals("emoji_emoji_1114211823741685761.png", commentEmojiInlineKey("[感谢]"))
            assertNotNull(Thread.currentThread().contextClassLoader.getResource("misc/emojis/emoji_1114211823741685761.png"))
        } finally {
            System.setProperty("user.dir", workingDirectory)
        }
    }
}
