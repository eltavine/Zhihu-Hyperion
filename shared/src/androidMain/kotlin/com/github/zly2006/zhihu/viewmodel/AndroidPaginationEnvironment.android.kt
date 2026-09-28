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

package com.github.zly2006.zhihu.viewmodel

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.ZhihuJson.json
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.navigation.requestLoginNavigation
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.platform.androidUserMessageSink
import com.github.zly2006.zhihu.util.HttpStatusException
import com.github.zly2006.zhihu.util.ResolvedCollectionHtmlExportItem
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.buildOfflineArticleExportHtml
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.exportCollectionItemsToZip
import com.github.zly2006.zhihu.util.saveBitmapToGallery
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.koin.mp.KoinPlatform
import java.io.File
import com.github.zly2006.zhihu.navigation.Article as ArticleDestination
import com.github.zly2006.zhihu.util.buildArticleExportHtml as buildAndroidArticleExportHtml

interface AndroidContextPaginationEnvironment : PaginationEnvironment {
    val context: Context
}

open class SharedAndroidPaginationEnvironment(
    override val context: Context,
    private val allowGuestAccess: Boolean,
) : AndroidContextPaginationEnvironment,
    CollectionContentEnvironment {
    private val contentFilterDatabase: ContentFilterDatabase = KoinPlatform.getKoin().get()
    private val settingsStore by lazy { androidSettingsStore(context) }
    private val userMessageSink by lazy { androidUserMessageSink(context) }

    override fun httpClient(): HttpClient {
        val loginForRecommendation = settingsStore.getBoolean("loginForRecommendation", true)
        if (allowGuestAccess && !loginForRecommendation) {
            return HttpClient {
                install(HttpCache)
                install(ContentNegotiation) {
                    json(json)
                }
                install(UserAgent) {
                    agent = AccountData.data.userAgent
                }
            }
        }
        return KoinPlatform
            .getKoin()
            .get<ZhihuAccountStore>()
            .client
            .httpClient()
    }

    override fun authenticatedCookies(): Map<String, String> {
        val loginForRecommendation = settingsStore.getBoolean("loginForRecommendation", true)
        return if (allowGuestAccess && !loginForRecommendation) {
            emptyMap()
        } else {
            AccountData.data.cookies
        }
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        if (error is HttpStatusException) {
            Log.e(tag, "Response: ${error.bodyText}", error)
            if (tryShowLoginExpiredDialog(error)) {
                return
            }
            showDebugErrorDialog(error)
        }
        Log.e(tag, "Failed to fetch feeds", error)
        userMessageSink.showShortMessage("加载失败: ${error.message}")
    }

    override suspend fun handleMobileHomeFeedFailure(error: Exception) {
        Log.e("AndroidHomeFeedViewModel", "Failed to fetch feeds", error)
        userMessageSink.showShortMessage("安卓端推荐加载失败: ${error.message}")
    }

    override suspend fun handleLocalRecommendationFailure(error: Exception) {
        Log.e("LocalHomeFeedViewModel", "Error fetching local feeds", error)
    }

    override suspend fun showLocalRecommendationDatabaseError() {
        withContext(Dispatchers.Main) {
            AlertDialog
                .Builder(context)
                .setTitle("数据库错误")
                .setMessage("本地推荐系统的数据库未正确初始化。请尝试重启应用或清除应用数据。")
                .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    override suspend fun exportCollectionItemsToHtmlZip(
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult {
        val outputDir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("外部文件目录不可用")
        val exportHttpClient = httpClient()
        val result = withContext(Dispatchers.IO) {
            exportCollectionItemsToZip(
                collectionTitle = collectionTitle,
                items = items,
                cacheDir = context.cacheDir,
                outputDir = outputDir,
                displayTitle = { item ->
                    item.content.title.ifBlank { item.content.description() }
                },
                resolveItem = { item ->
                    resolveCollectionItemForHtmlExport(
                        item = item,
                        exportHttpClient = exportHttpClient,
                        includeImages = includeImages,
                    )
                },
                onProgress = { progress ->
                    withContext(Dispatchers.Main) {
                        onProgress(
                            CollectionHtmlExportProgress(
                                totalCount = progress.totalCount,
                                processedCount = progress.processedCount,
                                successCount = progress.successCount,
                                skippedCount = progress.skippedCount,
                                failedCount = progress.failedCount,
                                currentTitle = progress.currentTitle,
                            ),
                        )
                    }
                },
            )
        }
        return CollectionHtmlExportResult(
            totalCount = result.totalCount,
            successCount = result.successCount,
            skippedCount = result.skippedCount,
            failedCount = result.failedCount,
            zipFilePath = result.zipFile?.absolutePath,
        )
    }

    override suspend fun handleCollectionExportFailure(error: Exception) {
        Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", error)
        userMessageSink.showShortMessage("导出失败: ${error.message}")
    }

    private suspend fun resolveCollectionItemForHtmlExport(
        item: CollectionItem,
        exportHttpClient: HttpClient,
        includeImages: Boolean,
    ): ResolvedCollectionHtmlExportItem? {
        val navDestination = item.content.navDestination as? ArticleDestination ?: return null
        val content = getOrFetchContentDetail(navDestination)
            ?: throw IllegalStateException("无法加载「${item.content.title}」详情")
        if (content !is DataHolder.Answer && content !is DataHolder.Article) {
            return null
        }

        return ResolvedCollectionHtmlExportItem(
            htmlFileName = buildArticleExportFileName(content, "html"),
            htmlContent = buildOfflineArticleExportHtml(
                context = context,
                content = content,
                includeAppAttribution = true,
                httpClient = exportHttpClient,
                includeImages = includeImages,
            ),
        )
    }

    private fun tryShowLoginExpiredDialog(error: HttpStatusException): Boolean {
        try {
            val body = json.parseToJsonElement(error.bodyText).jsonObject
            val errorBody = body["error"]?.jsonObject ?: return false
            if (errorBody["code"]?.jsonPrimitive?.int == 100 &&
                errorBody["message"]?.jsonPrimitive?.content == "ERR_TICKET_NOT_EXIST"
            ) {
                context.mainExecutor.execute {
                    if (context.canSafelyShowDialog()) {
                        AlertDialog
                            .Builder(context)
                            .setTitle("登录已过期")
                            .setMessage("请重新登录以继续使用完整功能。")
                            .setPositiveButton("重新登录") { _, _ ->
                                KoinPlatform.getKoin().get<ZhihuAccountStore>().clear()
                                requestLoginNavigation()
                            }.setNegativeButton("取消", null)
                            .show()
                    }
                }
                return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun showDebugErrorDialog(error: HttpStatusException) {
        context.mainExecutor.execute {
            if (context.canSafelyShowDialog()) {
                AlertDialog
                    .Builder(context)
                    .setTitle("错误 ${error.status}")
                    .setMessage(error.bodyText)
                    .setNeutralButton("复制curl") { _, _ ->
                        val curl = error.dumpedCurlRequest
                        context.clipboardManager
                            .setPrimaryClip(
                                ClipData.newPlainText(
                                    "curl",
                                    curl,
                                ),
                            )
                        userMessageSink.showShortMessage("已复制到剪贴板")
                    }.show()
            }
        }
    }

    // Export methods
    override fun setPlainTextClipboard(
        label: String,
        text: String,
    ) {
        context.clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = buildAndroidArticleExportHtml(
        context = context,
        content = content,
        includeAppAttribution = includeAppAttribution,
        extraSectionsHtml = extraSectionsHtml,
    )

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = buildOfflineArticleExportHtml(
        context = context,
        content = content,
        includeAppAttribution = includeAppAttribution,
        httpClient = httpClient,
    )

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = saveBitmapToGallery(context, displayName, bitmap as android.graphics.Bitmap)

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Zhihu++")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("无法创建下载文件")

            return try {
                resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write(htmlContent)
                } ?: throw IllegalStateException("无法打开下载文件")

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                "Zhihu++/$displayName"
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }

        @Suppress("DEPRECATION")
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Zhihu++",
        )
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
            throw IllegalStateException("无法创建下载目录")
        }
        val file = File(downloadsDir, displayName)
        file.writeText(htmlContent)
        return file.absolutePath
    }

    override fun articleImageExportRenderer(): ArticleImageExportRenderer =
        AndroidArticleExportRenderer(context)

    override fun hasImageExportPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    override fun requiresHtmlExportPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun requestImageExportPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1001,
            )
        }
    }

    override fun loadExportAssetText(fileName: String): String =
        context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
}

fun PaginationViewModel<*>.paginationEnvironment(context: Context): AndroidContextPaginationEnvironment =
    SharedAndroidPaginationEnvironment(context, allowGuestAccess)

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment {
    val context = LocalContext.current
    return remember(context, allowGuestAccess) { SharedAndroidPaginationEnvironment(context, allowGuestAccess) }
}

fun PaginationViewModel<*>.refresh(context: Context) {
    refresh(paginationEnvironment(context))
}

fun PaginationViewModel<*>.loadMore(context: Context) {
    loadMore(paginationEnvironment(context))
}

fun PaginationViewModel<*>.httpClient(context: Context): HttpClient =
    paginationEnvironment(context).httpClient()

private fun Context.canSafelyShowDialog(): Boolean {
    val activity = this as? Activity ?: return false
    if (activity.isFinishing || activity.isDestroyed) return false
    val lifecycleOwner = activity as? LifecycleOwner ?: return true
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
