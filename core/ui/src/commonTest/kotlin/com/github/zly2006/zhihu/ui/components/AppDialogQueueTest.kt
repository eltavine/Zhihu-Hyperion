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

import kotlin.test.Test
import kotlin.test.assertEquals

class AppDialogQueueTest {
    private fun request(key: String) = AppDialogRequest(key, title = key, text = "", confirm = AppDialogAction("知道了"))

    @Test
    fun requestsThatFailTogetherShowOneDialogPerKey() {
        val queue = AppDialogQueue()

        repeat(3) { queue.show(request("login-expired")) }

        assertEquals(listOf("login-expired"), queue.requests.value.map { it.key })
    }

    @Test
    fun dismissingTheVisibleDialogShowsTheNextOne() {
        val queue = AppDialogQueue()
        queue.show(request("crash-report"))
        queue.show(request("login-expired"))

        queue.dismiss("crash-report")

        assertEquals(listOf("login-expired"), queue.requests.value.map { it.key })
    }
}
