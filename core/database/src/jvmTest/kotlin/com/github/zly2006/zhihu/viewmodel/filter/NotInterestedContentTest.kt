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

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class NotInterestedContentTest {
    @Test
    fun recordsSurviveReopenAndHistoryResetAndCanBeUndone() = runBlocking {
        val file = createTempDirectory("not-interested").resolve("content-filter.db").toFile()
        val database = getContentFilterDatabase(file)
        try {
            val dao = database.notInterestedContentDao()
            dao.insert(NotInterestedContent("answer:70"))
            dao.insert(NotInterestedContent("answer:70"))
            dao.insert(NotInterestedContent("article:70"))
            database.contentFilterDao().clearAllRecords()
            assertEquals(setOf("answer:70", "article:70"), dao.observeContentKeys().first().toSet())
        } finally {
            database.close()
        }
        val reopened = getContentFilterDatabase(file)
        try {
            val dao = reopened.notInterestedContentDao()
            assertEquals(setOf("answer:70", "article:70"), dao.observeContentKeys().first().toSet())
            dao.delete("answer:70")
            assertEquals(listOf("article:70"), dao.observeContentKeys().first())
        } finally {
            reopened.close()
        }
    }

    @Test
    fun migrationFromVersion8PreservesExistingRules() = runBlocking {
        val file = createTempDirectory("not-interested-migration").resolve("content-filter.db").toFile()
        val schema = Json
            .parseToJsonElement(
                File("schemas/com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase/8.json").readText(),
            ).jsonObject
            .getValue("database")
            .jsonObject
        BundledSQLiteDriver().open(file.absolutePath).use { connection ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val table = entity.jsonObject
                val name = table.getValue("tableName").jsonPrimitive.content
                connection.execSQL(
                    table
                        .getValue("createSql")
                        .jsonPrimitive.content
                        .replace("\${TABLE_NAME}", name),
                )
                table["indices"]?.jsonArray.orEmpty().forEach { index ->
                    connection.execSQL(
                        index.jsonObject
                            .getValue("createSql")
                            .jsonPrimitive.content
                            .replace("\${TABLE_NAME}", name),
                    )
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
            connection.execSQL("INSERT INTO blocked_users (userId, userName, createdTime) VALUES ('kept', '保留的用户', 1)")
            connection.execSQL("PRAGMA user_version = 8")
        }
        val database = getContentFilterDatabase(file)
        try {
            assertEquals(listOf("kept"), database.blockedUserDao().getAllUsers().map { it.userId })
            val dao = database.notInterestedContentDao()
            assertEquals(emptyList(), dao.observeContentKeys().first())
            dao.insert(NotInterestedContent("question:70"))
            assertEquals(listOf("question:70"), dao.observeContentKeys().first())
        } finally {
            database.close()
        }
    }
}
