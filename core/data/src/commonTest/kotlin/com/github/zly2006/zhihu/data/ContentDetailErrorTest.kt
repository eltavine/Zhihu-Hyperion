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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.util.ZhihuApiErrorException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ContentDetailErrorTest {
    /** 未登录请求回答详情时知乎返回的原始响应（HTTP 403）。 */
    private val loginRequiredBody = Json
        .parseToJsonElement(
            """{"error":{"need_login":true,"redirect":"https://www.zhihu.com/account/unhuman?type=U4E3Z1&need_login=true","code":40353,"message":"请您登录后查看更多专业优质内容。"}}""",
        ).jsonObject

    @Test
    fun loginRequiredResponseIsReportedInsteadOfDecodedAsAnAnswer() = runTest {
        val error = assertFailsWith<ZhihuApiErrorException> {
            fetchZhihuContentDetail(Article(type = ArticleType.Answer, id = 2011835173877092640)) { _, _ -> loginRequiredBody }
        }

        assertEquals("请您登录后查看更多专业优质内容。", error.message)
        assertTrue(error.needLogin)
    }
}
