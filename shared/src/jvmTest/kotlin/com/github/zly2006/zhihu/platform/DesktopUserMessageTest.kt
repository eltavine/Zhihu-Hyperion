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

package com.github.zly2006.zhihu.platform

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DesktopUserMessageTest {
    /** Windows、Linux 版的复制、保存、导入等结果提示原来只打印到标准输出，窗口里看不到。 */
    @Test
    fun userMessageShowsInTheDesktopWindow() = runComposeUiTest {
        setContent {
            SnackbarUserMessageHost {
                val userMessages = rememberUserMessageSink()
                LaunchedEffect(userMessages) {
                    userMessages.showShortMessage("已复制链接")
                }
            }
        }
        waitUntilExactlyOneExists(hasText("已复制链接"))
    }
}
