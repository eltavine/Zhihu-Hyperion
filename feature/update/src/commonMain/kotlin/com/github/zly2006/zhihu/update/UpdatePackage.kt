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

/** 能否在应用内下载并安装更新包；不支持的平台由界面在浏览器中打开下载地址。 */
expect val isInAppUpdateInstallSupported: Boolean

/** 把 [asset] 下载到应用缓存，并按 update.json 中的 SHA-256 校验。 */
internal expect suspend fun downloadUpdatePackage(client: HttpClient, asset: UpdateAsset)

/** 用系统安装器打开 [downloadUpdatePackage] 下载的安装包。 */
internal expect fun installDownloadedUpdatePackage()
