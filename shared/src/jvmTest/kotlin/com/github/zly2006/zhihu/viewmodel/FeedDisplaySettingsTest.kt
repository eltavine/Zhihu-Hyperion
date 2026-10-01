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

package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.data.ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.QUALITY_FILTER_MODE_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.QualityFilterMode
import com.github.zly2006.zhihu.platform.MapSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedDisplaySettingsTest {
    @Test
    fun desktopAppliesSavedQualityFilterSettings() {
        val settings = MapSettingsStore().apply {
            putString(QUALITY_FILTER_MODE_PREFERENCE_KEY, QualityFilterMode.HIDE.name)
            putInt(ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY, 42)
            putBoolean("reverseBlock", true)
        }

        val display = settings.toFeedDisplaySettings()

        assertEquals(QualityFilterMode.HIDE, display.qualityFilterMode)
        assertEquals(42, display.qualityFilter.answerVoteupCount)
        assertTrue(display.reverseBlock)
    }
}
