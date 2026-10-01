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

package com.github.zly2006.zhihu.update

import io.ktor.client.HttpClient

actual val isInAppUpdateInstallSupported: Boolean = false

internal actual suspend fun downloadUpdatePackage(client: HttpClient, asset: UpdateAsset): Unit =
    throw UnsupportedOperationException("当前平台不支持在应用内下载更新包")

internal actual fun installDownloadedUpdatePackage(): Unit =
    throw UnsupportedOperationException("当前平台不支持在应用内安装更新包")
