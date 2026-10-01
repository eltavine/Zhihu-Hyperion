/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Co-author: eltavine <me@eltavine.com>
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.desktop.desktopZhihuDataFile
import com.github.zly2006.zhihu.desktop.desktopZhihuDownloadsDir
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.buildCollectionExportZipFileName
import com.github.zly2006.zhihu.util.sanitizeArticleExportFileNamePart
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.swing.JEditorPane
import javax.swing.SwingUtilities
import com.github.zly2006.zhihu.util.buildArticleExportHtml as buildSharedArticleExportHtml
import com.github.zly2006.zhihu.util.buildOfflineArticleExportHtml as buildSharedOfflineArticleExportHtml

/** 桌面的文章与收藏夹导出：写入用户下载目录，长图由 Swing 渲染导出 HTML。 */
class DesktopContentExporter : ContentExporter {
    override fun hasImageExportPermission(): Boolean = true

    override fun requiresHtmlExportPermission(): Boolean = false

    override fun requestImageExportPermission() = Unit

    override fun loadExportAssetText(fileName: String): String =
        checkNotNull(javaClass.classLoader.getResourceAsStream(fileName)) { "桌面导出资源缺失：$fileName" }
            .bufferedReader()
            .use { it.readText() }

    override fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = buildSharedArticleExportHtml(
        loadAssetText = ::loadExportAssetText,
        content = content,
        includeAppAttribution = includeAppAttribution,
        extraSectionsHtml = extraSectionsHtml,
    )

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = buildSharedOfflineArticleExportHtml(
        loadAssetText = ::loadExportAssetText,
        content = content,
        includeAppAttribution = includeAppAttribution,
        httpClient = httpClient,
        useOriginalOnImageFetchFailure = true,
    )

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String {
        val downloadsDir = desktopZhihuDownloadsDir()
        val file = File(downloadsDir, displayName)
        file.writeText(htmlContent)
        return file.absolutePath
    }

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) {
        val downloadsDir = desktopZhihuDownloadsDir()
        val file = File(downloadsDir, displayName)
        writeJpegImage(file, (bitmap as BufferedImage).toJpegImage())
    }

    override fun articleImageExportRenderer(): ArticleImageExportRenderer =
        DesktopArticleExportRenderer()

    override suspend fun exportCollectionItemsToHtmlZip(
        environment: ZhihuApiEnvironment,
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult {
        val timestampMillis = System.currentTimeMillis()
        val stagingDir = File(
            desktopZhihuDataFile("collection-html-export-cache").also { directory ->
                if (!directory.exists()) {
                    directory.mkdirs()
                }
            },
            "collection_html_export_${sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "collection" }}_$timestampMillis",
        )
        if (stagingDir.exists()) {
            stagingDir.deleteRecursively()
        }
        if (!stagingDir.mkdirs()) {
            throw IllegalStateException("无法创建导出缓存目录")
        }

        val outputDir = desktopZhihuDownloadsDir("无法创建导出 ZIP 目录")
        val exportHttpClient = environment.httpClient()

        var processedCount = 0
        var successCount = 0
        var skippedCount = 0
        var failedCount = 0
        var currentTitle = ""

        suspend fun emitProgress() {
            onProgress(
                CollectionHtmlExportProgress(
                    totalCount = items.size,
                    processedCount = processedCount,
                    successCount = successCount,
                    skippedCount = skippedCount,
                    failedCount = failedCount,
                    currentTitle = currentTitle,
                ),
            )
        }

        emitProgress()

        items.forEach { item ->
            currentTitle = item.content.title
            try {
                val content = item.resolveDesktopExportContent(environment)
                if (content == null) {
                    skippedCount++
                } else {
                    val htmlContent = buildSharedOfflineArticleExportHtml(
                        loadAssetText = ::loadExportAssetText,
                        content = content,
                        includeAppAttribution = true,
                        httpClient = exportHttpClient,
                        includeImages = includeImages,
                        useOriginalOnImageFetchFailure = true,
                    )
                    File(stagingDir, buildArticleExportFileName(content, "html")).writeText(htmlContent)
                    successCount++
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failedCount++
                Log.e("CollectionContentViewModel", "Failed to export collection item: ${item.content.title}", e)
            } finally {
                processedCount++
                emitProgress()
            }
        }

        val zipFile = if (successCount > 0) {
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw IllegalStateException("无法创建导出 ZIP 目录")
            }
            File(outputDir, buildCollectionExportZipFileName(collectionTitle, timestampMillis)).also { file ->
                if (file.exists()) {
                    file.delete()
                }
                zipDirectoryContents(stagingDir, file)
            }
        } else {
            null
        }

        return CollectionHtmlExportResult(
            totalCount = items.size,
            successCount = successCount,
            skippedCount = skippedCount,
            failedCount = failedCount,
            zipFilePath = zipFile?.absolutePath,
        )
    }

    override suspend fun handleCollectionExportFailure(error: Exception) {
        Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", error)
    }
}

@Composable
actual fun rememberContentExporter(): ContentExporter = remember { DesktopContentExporter() }

private data class DesktopPreparedExportContent(
    val htmlContent: String,
) : PreparedArticleExportContent

private class DesktopArticleExportRenderer : ArticleImageExportRenderer {
    override suspend fun prepareExportWebView(
        htmlContent: String,
        timeoutMs: Long,
    ): PreparedArticleExportContent = DesktopPreparedExportContent(htmlContent)

    override suspend fun captureExportBitmap(preparedWebView: PreparedArticleExportContent): Any =
        withContext(Dispatchers.IO) {
            preparedWebView as DesktopPreparedExportContent
            renderHtmlToImage(preparedWebView.htmlContent)
        }

    override suspend fun destroyExportWebView(preparedWebView: PreparedArticleExportContent) = Unit

    override fun recycleExportBitmap(bitmap: Any) = Unit

    private fun renderHtmlToImage(htmlContent: String): BufferedImage = runOnSwingThread {
        val viewportWidthPx = 900
        val editorPane = JEditorPane("text/html", htmlContent).apply {
            isEditable = false
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            setSize(viewportWidthPx, Int.MAX_VALUE / 4)
        }
        val preferredSize = editorPane.preferredSize
        val contentHeightPx = preferredSize.height.coerceAtLeast(1)
        editorPane.setSize(viewportWidthPx, contentHeightPx)
        editorPane.validate()

        BufferedImage(
            viewportWidthPx,
            contentHeightPx,
            BufferedImage.TYPE_INT_ARGB,
        ).also { image ->
            val graphics = image.createGraphics()
            try {
                graphics.color = java.awt.Color.WHITE
                graphics.fillRect(0, 0, viewportWidthPx, contentHeightPx)
                editorPane.paint(graphics)
            } finally {
                graphics.dispose()
            }
        }
    }
}

private fun BufferedImage.toJpegImage(): BufferedImage {
    if (type == BufferedImage.TYPE_INT_RGB) {
        return this
    }
    return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { image ->
        val graphics = image.createGraphics()
        try {
            graphics.color = java.awt.Color.WHITE
            graphics.fillRect(0, 0, width, height)
            graphics.drawImage(this, 0, 0, null)
        } finally {
            graphics.dispose()
        }
    }
}

private fun writeJpegImage(file: File, image: BufferedImage) {
    val writers = ImageIO.getImageWritersByFormatName("jpg")
    if (!writers.hasNext()) {
        throw IllegalStateException("No JPEG writer available")
    }
    val writer = writers.next()
    ImageIO.createImageOutputStream(file).use { output ->
        writer.output = output
        try {
            val params = writer.defaultWriteParam
            if (params.canWriteCompressed()) {
                params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                params.compressionQuality = 0.80f
            }
            writer.write(null, IIOImage(image, null, null), params)
        } finally {
            writer.dispose()
        }
    }
}

private fun <T> runOnSwingThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    var value: Any? = null
    var error: Throwable? = null
    SwingUtilities.invokeAndWait {
        try {
            value = block()
        } catch (throwable: Throwable) {
            error = throwable
        }
    }
    error?.let { throw it }
    @Suppress("UNCHECKED_CAST")
    return value as T
}

private suspend fun CollectionItem.resolveDesktopExportContent(
    environment: ZhihuApiEnvironment,
): DataHolder.Content? {
    val destination = content.navDestination as? Article ?: return null
    return environment.fetchContentDetail(destination)
}

private suspend fun zipDirectoryContents(
    sourceDir: File,
    zipFile: File,
) = withContext(Dispatchers.IO) {
    ZipOutputStream(zipFile.outputStream().buffered()).use { outputStream ->
        sourceDir
            .listFiles()
            ?.sortedBy { it.name }
            ?.forEach { file ->
                addFileToZip(
                    file = file,
                    entryPrefix = "",
                    outputStream = outputStream,
                )
            }
    }
}

private fun addFileToZip(
    file: File,
    entryPrefix: String,
    outputStream: ZipOutputStream,
) {
    if (file.isDirectory) {
        val nextPrefix = if (entryPrefix.isBlank()) file.name else "$entryPrefix/${file.name}"
        file
            .listFiles()
            ?.sortedBy { it.name }
            ?.forEach { child ->
                addFileToZip(
                    file = child,
                    entryPrefix = nextPrefix,
                    outputStream = outputStream,
                )
            }
        return
    }

    val entryName = if (entryPrefix.isBlank()) file.name else "$entryPrefix/${file.name}"
    outputStream.putNextEntry(ZipEntry(entryName))
    file.inputStream().buffered().use { inputStream ->
        inputStream.copyTo(outputStream)
    }
    outputStream.closeEntry()
}
