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

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 本地浏览历史的一条记录。再次访问同一目的地会以新的 [id] 替换旧行，因此按 [id] 倒序即最近访问在前。
 */
@Entity(
    tableName = BrowsingHistoryEntry.TABLE_NAME,
    indices = [Index(value = ["destinationKey"], unique = true)],
)
data class BrowsingHistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 目的地的去重身份，与导航目的地的相等语义一致，例如 `answer:1`。 */
    val destinationKey: String,
    /** 序列化后的导航目的地，保留标题、作者等展示信息。 */
    val destinationJson: String,
) {
    companion object {
        const val TABLE_NAME = "browsing_history"
    }
}
