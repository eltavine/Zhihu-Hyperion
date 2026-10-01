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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class GenderTest {
    @Test
    fun missingNullAndUndefinedCodesAreUnknownRatherThanFemale() {
        assertEquals(Gender.Unknown, decodePerson(genderField = "").gender)
        assertEquals(Gender.Unknown, decodePerson(genderField = """"gender": null,""").gender)
        assertEquals(Gender.Unknown, decodePerson(genderField = """"gender": 2,""").gender)
        assertEquals(Gender.Female, decodePerson(genderField = """"gender": 0,""").gender)
    }

    @Test
    fun encodingKeepsTheZhihuIntegerCode() {
        val person = decodePerson(genderField = """"gender": 1,""")

        val encoded = ZhihuJson.json.encodeToJsonElement(Person.serializer(), person).jsonObject

        assertEquals(Gender.Male, person.gender)
        assertEquals(1, encoded.getValue("gender").jsonPrimitive.int)
        assertEquals(person, ZhihuJson.decodeJson<Person>(encoded))
    }

    private fun decodePerson(genderField: String): Person = ZhihuJson.decodeJson(
        Json.parseToJsonElement(
            """
            {
              "id": "author-id",
              "url": "https://api.zhihu.com/people/author-id",
              "user_type": "people",
              "name": "作者",
              $genderField
              "headline": "",
              "avatar_url": "https://pic.example/avatar.jpg"
            }
            """.trimIndent(),
        ),
    )
}
