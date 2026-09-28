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

package com.github.zly2006.zhihu.viewmodel.filter

class FakeBlockedUserDao(
    vararg blockedUserIds: String,
) : BlockedUserDao {
    private val users = blockedUserIds.associateWithTo(linkedMapOf()) { BlockedUser(userId = it, userName = it) }

    override suspend fun getAllUsers() = users.values.toList()

    override suspend fun getAllUserIds() = users.keys.toList()

    override suspend fun insertUser(user: BlockedUser) {
        users[user.userId] = user
    }

    override suspend fun deleteUserById(userId: String) {
        users.remove(userId)
    }

    override suspend fun clearAllUsers() = users.clear()

    override suspend fun getUserCount() = users.size

    override suspend fun isUserBlocked(userId: String) = userId in users
}

class FakeBlockedQuestionAuthorDao : BlockedQuestionAuthorDao {
    private val authors = linkedMapOf<String, BlockedQuestionAuthor>()

    override suspend fun getAllUsers() = authors.values.toList()

    override suspend fun insertUser(user: BlockedQuestionAuthor) {
        authors[user.userId] = user
    }

    override suspend fun deleteUserById(userId: String) {
        authors.remove(userId)
    }

    override suspend fun clearAllUsers() = authors.clear()

    override suspend fun getUserCount() = authors.size

    override suspend fun isUserBlocked(userId: String) = userId in authors
}
