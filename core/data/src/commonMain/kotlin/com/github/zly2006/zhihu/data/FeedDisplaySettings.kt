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

package com.github.zly2006.zhihu.data

data class FeedDisplaySettings(
    val qualityFilterMode: QualityFilterMode = QualityFilterMode.RULES,
    val qualityFilter: QualityFilterSettings = QualityFilterSettings(),
    val reverseBlock: Boolean = false,
)

enum class QualityFilterMode {
    OFF,
    RULES,
    HIDE,
}

const val QUALITY_FILTER_MODE_PREFERENCE_KEY = "qualityFilterMode"
const val ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY = "answerVoteupThreshold"
const val ARTICLE_VOTEUP_THRESHOLD_PREFERENCE_KEY = "articleVoteupThreshold"
const val ARTICLE_FOLLOWERS_THRESHOLD_PREFERENCE_KEY = "articleFollowersThreshold"
const val VIDEO_VOTE_THRESHOLD_PREFERENCE_KEY = "videoVoteThreshold"
const val VIDEO_FOLLOWERS_THRESHOLD_PREFERENCE_KEY = "videoFollowersThreshold"
const val QUESTION_ANSWER_THRESHOLD_PREFERENCE_KEY = "questionAnswerThreshold"
const val QUESTION_FOLLOWERS_THRESHOLD_PREFERENCE_KEY = "questionFollowersThreshold"
