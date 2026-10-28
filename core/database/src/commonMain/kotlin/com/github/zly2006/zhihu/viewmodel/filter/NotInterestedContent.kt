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

package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 用户主动排除的首页推荐，以内容类型与 ID 标识，独立于曝光、已读和自动过滤开关。 */
@Entity(tableName = "not_interested_contents")
data class NotInterestedContent(
    @PrimaryKey val contentKey: String,
)

@Dao
interface NotInterestedContentDao {
    @Query("SELECT contentKey FROM not_interested_contents")
    fun observeContentKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(content: NotInterestedContent)

    @Query("DELETE FROM not_interested_contents WHERE contentKey = :contentKey")
    suspend fun delete(contentKey: String)
}
