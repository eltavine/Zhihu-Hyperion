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

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

actual val isInAppUpdateInstallSupported: Boolean = true

/** 位于 `res/xml/file_paths.xml` 共享的缓存目录，系统安装器经 FileProvider 读取。 */
private fun Context.updatePackageFile() = File(cacheDir, "update.apk")

internal actual suspend fun downloadUpdatePackage(client: HttpClient, asset: UpdateAsset) {
    val file = KoinPlatform.getKoin().get<Context>().updatePackageFile()
    val digest = MessageDigest.getInstance("SHA-256")
    withContext(Dispatchers.IO) {
        client.prepareGet(asset.url).execute { response ->
            check(response.status.isSuccess()) { "安装包返回 HTTP ${response.status.value}" }
            DigestInputStream(response.bodyAsChannel().toInputStream(), digest).use { input ->
                file.outputStream().use(input::copyTo)
            }
        }
    }
    val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
    if (!sha256.equals(asset.sha256, ignoreCase = true)) {
        file.delete()
        error("安装包校验失败，请重新下载")
    }
}

internal actual fun installDownloadedUpdatePackage() {
    val context = KoinPlatform.getKoin().get<Context>()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", context.updatePackageFile())
    context.startActivity(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )
}
