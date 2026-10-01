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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BrowsingHistoryDao {
    @Query("SELECT destinationJson FROM ${BrowsingHistoryEntry.TABLE_NAME} ORDER BY id DESC")
    suspend fun getDestinationsNewestFirst(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BrowsingHistoryEntry)

    @Query("DELETE FROM ${BrowsingHistoryEntry.TABLE_NAME} WHERE destinationKey = :destinationKey")
    suspend fun delete(destinationKey: String)

    @Query(
        """
        DELETE FROM ${BrowsingHistoryEntry.TABLE_NAME}
        WHERE id NOT IN (SELECT id FROM ${BrowsingHistoryEntry.TABLE_NAME} ORDER BY id DESC LIMIT :limit)
        """,
    )
    suspend fun keepNewest(limit: Int)

    @Query("DELETE FROM ${BrowsingHistoryEntry.TABLE_NAME}")
    suspend fun clear()
}
