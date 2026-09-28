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

package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.platform.MapSettingsStore
import com.github.zly2006.zhihu.util.AIGC_VOTE_SERVER_URL_KEY
import com.github.zly2006.zhihu.util.TextDocumentStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AigcVoteServiceTest {
    @Test
    fun clientIdIsGeneratedOnceAndBlankServerUrlFallsBackToDefault() {
        val settings = MapSettingsStore().apply { putString(AIGC_VOTE_SERVER_URL_KEY, " ") }
        val engine = MockEngine { respond("") }
        val store = ZhihuAccountStore(ZhihuAccountRepository(MemoryDocument()), engine)
        AigcVoteService(settings, store, engine).use { service ->
            val clientId = service.clientId()

            assertTrue(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(clientId))
            assertEquals(clientId, service.clientId())
            assertEquals("https://aigc-vote.ai.fintechedu.cn", service.baseUrl())
            assertNull(service.voter())
        }
        store.close()
    }

    private class MemoryDocument : TextDocumentStore {
        private var text: String? = null

        override fun readText() = text

        override fun writeText(text: String) {
            this.text = text
        }

        override fun delete() {
            text = null
        }
    }
}
