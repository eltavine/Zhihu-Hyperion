/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.zly2006.zhihu.navigation.AndroidArticleNavigationHandoff
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.ui.article.prepareContentDocument
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.update.InstalledBuild
import com.github.zly2006.zhihu.util.EmojiManager
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.createEmojiInlineContent
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.encodeBlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.importBlocklistBackupFromJsonText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.koin.compose.koinInject
import java.io.File

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberAppVersionInfo(): String {
    val build = koinInject<InstalledBuild>()
    val context = LocalContext.current
    val buildType = runCatching {
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData
            ?.getString("com.github.zly2006.zhihu.BUILD_TYPE")
    }.getOrNull() ?: if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) "debug" else "release"
    return "${build.versionName} $buildType, ${build.commit}"
}

@Composable
actual fun consumePendingCommentId(content: com.github.zly2006.zhihu.navigation.NavDestination): String? {
    val articleNavigationHandoff = koinInject<AndroidArticleNavigationHandoff>()
    return remember(content) { articleNavigationHandoff.consumeCommentId(content) }
}

@Composable
actual fun ArticleWebViewContent(
    article: Article,
    html: String,
    title: String,
    scrollState: ScrollState,
    rememberedScrollY: Int,
    rememberedScrollYSync: Boolean,
    onRememberedScrollYSyncChange: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    WebviewComp(
        onDoubleTap = onDoubleTap,
        scrollState = scrollState,
    ) {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient {
            if (!rememberedScrollYSync) {
                coroutineScope.launch {
                    while (scrollState.maxValue < rememberedScrollY) {
                        delay(100)
                    }
                    Log.i("zhihu-scroll", "scroll to $rememberedScrollY, max= ${scrollState.maxValue}, sync on")
                    scrollState.animateScrollTo(rememberedScrollY)
                    onRememberedScrollYSyncChange(true)
                }
            }
        }
        it.contentId = article.id.toString()
        it.loadZhihu(
            "https://www.zhihu.com/${article.type}/${article.id}",
            prepareContentDocument(html, onImageLoadFailed),
            title,
        )
    }
}

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = fuckHonorService()

@Composable
actual fun rememberHomeIsDebuggable(): Boolean {
    val context = LocalContext.current
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Composable
actual fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
    onImported: (String) -> Unit,
): BlocklistRuleImporter {
    val context = LocalContext.current
    val database = koinInject<ContentFilterDatabase>()
    val coroutineScope = rememberCoroutineScope()
    val currentOnImported by rememberUpdatedState(onImported)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val summary = withContext(Dispatchers.IO) {
                        val text = context.contentResolver
                            .openInputStream(uri)
                            ?.bufferedReader()
                            ?.readText()
                            ?: return@withContext "读取文件失败"
                        importBlocklistBackupFromJsonText(
                            keywordDao = database.blockedKeywordDao(),
                            userDao = database.blockedUserDao(),
                            questionAuthorDao = database.blockedQuestionAuthorDao(),
                            topicDao = database.blockedTopicDao(),
                            text = text,
                        )
                    }
                    currentOnImported(summary)
                } catch (e: Exception) {
                    Log.e("BlocklistSettings", "Failed to import blocklist", e)
                    userMessages.showShortMessage("导入失败: ${e.message}")
                }
            }
        }
    }
    return remember(importLauncher) {
        object : BlocklistRuleImporter {
            override fun invoke() = importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }
}

@Composable
actual fun rememberBlocklistRuleExporter(): BlocklistRuleExporter {
    val context = LocalContext.current
    val database = koinInject<ContentFilterDatabase>()
    return remember(context, database) {
        object : BlocklistRuleExporter {
            override suspend fun invoke(): String {
                val file = withContext(Dispatchers.IO) {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = File(dir, "zhihu_hyperion_blocklist.json")
                    file.writeText(
                        encodeBlocklistBackup(
                            keywordDao = database.blockedKeywordDao(),
                            userDao = database.blockedUserDao(),
                            questionAuthorDao = database.blockedQuestionAuthorDao(),
                            topicDao = database.blockedTopicDao(),
                        ),
                    )
                    file
                }
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file,
                        ),
                        "application/json",
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "查看屏蔽规则"))
                return "已导出到 ${file.absolutePath}"
            }
        }
    }
}

@Composable
actual fun ZhihuHtmlWebViewContent(html: String) {
    WebviewComp {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient()
        it.loadZhihu(
            "https://www.zhihu.com",
            Jsoup.parse(html),
        )
    }
}

@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> =
    remember(emojiKeys) { createEmojiInlineContent(emojiKeys) }

@Composable
actual fun rememberCommentEmojis(): List<CommentEmoji> {
    val placeholders by EmojiManager.placeholders.collectAsState()
    return remember(placeholders) {
        placeholders.mapNotNull { placeholder ->
            commentEmojiInlineKey(placeholder)?.let { inlineKey ->
                CommentEmoji(placeholder = placeholder, inlineKey = inlineKey)
            }
        }
    }
}

actual fun commentEmojiInlineKey(placeholder: String): String? {
    val emojiPath = EmojiManager.getEmojiPath(placeholder) ?: return null
    val emojiFileName = emojiPath.substringAfterLast('/')
    return "emoji_$emojiFileName"
}

actual fun Modifier.commentSelectionWorkaround(): Modifier = fuckHonorService()

@Composable
actual fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
) {
    WebviewComp {
        it.loadZhihu(
            "https://www.zhihu.com/question/$questionId",
            Jsoup.parse(html),
        )
    }
}

actual fun Modifier.questionSelectionWorkaround(): Modifier = fuckHonorService()

@Composable
actual fun ArticleImmersiveModeEffect(immersive: Boolean) {
    val context = LocalContext.current
    val window = remember(context) { (context as? Activity)?.window }
    LaunchedEffect(window, immersive) {
        window?.let { w ->
            val ctrl = WindowInsetsControllerCompat(w, w.decorView)
            if (immersive) {
                ctrl.hide(WindowInsetsCompat.Type.statusBars())
                ctrl.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                ctrl.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
}

@Composable
actual fun LeaveImmersiveModeCleanup() {
    val context = LocalContext.current
    val window = remember(context) { (context as? Activity)?.window }
    LaunchedEffect(window) {
        window?.let { w ->
            WindowInsetsControllerCompat(w, w.decorView)
                .show(WindowInsetsCompat.Type.statusBars())
        }
    }
}
