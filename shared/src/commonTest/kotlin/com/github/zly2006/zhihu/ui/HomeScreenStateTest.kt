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

import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.viewmodel.filter.NotInterestedContent
import com.github.zly2006.zhihu.viewmodel.filter.NotInterestedContentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenStateTest {
    @Test
    fun filterKeysStayLoadedWhileHomeCompositionIsAbsent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dao = FakeNotInterestedContentDao()
        val state = HomeScreenState(dao)
        try {
            // The first home composition waits for the database to emit.
            assertNull(state.notInterestedKeys)
            runCurrent()
            assertEquals(setOf("answer:1"), state.notInterestedKeys)

            // The Home composable may be disposed when an answer is opened.
            // No UI collector is active here; the ViewModel must retain its last value.
            assertEquals(setOf("answer:1"), state.notInterestedKeys)

            // Database updates should still be reflected while the home UI is absent.
            dao.keys.value = listOf("answer:1", "article:2")
            runCurrent()
            assertEquals(setOf("answer:1", "article:2"), state.notInterestedKeys)
        } finally {
            state.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class FakeNotInterestedContentDao : NotInterestedContentDao {
        val keys = MutableStateFlow(listOf("answer:1"))

        override fun observeContentKeys(): Flow<List<String>> = keys

        override suspend fun insert(content: NotInterestedContent) {
            keys.value = keys.value + content.contentKey
        }

        override suspend fun delete(contentKey: String) {
            keys.value = keys.value.filterNot { it == contentKey }
        }
    }
}
