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

package com.github.zly2006.zhihu.util

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

/** 单个文本文档的持久化位置，例如账户列表或本地浏览历史；测试可换成内存实现。 */
interface TextDocumentStore {
    fun readText(): String?

    fun writeText(text: String)

    fun delete()
}

/** 先写同目录临时文件再原子替换，写入中途崩溃不会留下截断的文件。 */
class AtomicTextFile(
    private val file: Path,
) : TextDocumentStore {
    override fun readText(): String? = if (SystemFileSystem.exists(file)) {
        SystemFileSystem.source(file).buffered().use { it.readString() }
    } else {
        null
    }

    override fun writeText(text: String) {
        file.parent?.let { SystemFileSystem.createDirectories(it) }
        val temporaryFile = Path("$file.tmp")
        SystemFileSystem.sink(temporaryFile).buffered().use { it.writeString(text) }
        SystemFileSystem.atomicMove(temporaryFile, file)
    }

    override fun delete() = SystemFileSystem.delete(file, mustExist = false)
}
