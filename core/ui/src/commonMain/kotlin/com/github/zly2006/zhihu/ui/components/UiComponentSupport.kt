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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedQuestionAuthor
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.KeywordType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.koin.compose.koinInject

expect suspend fun extractFeedKeywords(
    title: String,
    excerpt: String?,
): List<KeywordWithWeight>

expect val feedKeywordExtractionAvailable: Boolean

@Composable
fun FeedAuthorBlockConfirmDialog(
    request: FeedAuthorBlockRequest?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val database = koinInject<ContentFilterDatabase>()
    val environment = koinInject<ZhihuApiEnvironment>()
    var questionAuthorStats by remember(request) { mutableStateOf<QuestionAuthorActivityStats?>(null) }
    var isQuestionAuthorStatsLoading by remember(request) {
        mutableStateOf(request?.type == FeedAuthorBlockType.QUESTION_AUTHOR)
    }

    LaunchedEffect(request) {
        if (request?.type != FeedAuthorBlockType.QUESTION_AUTHOR) return@LaunchedEffect

        isQuestionAuthorStatsLoading = true
        questionAuthorStats = try {
            environment
                .fetchJson(
                    "https://api.zhihu.com/people/${request.userId}",
                    "answer_count,question_count",
                )?.let { profile ->
                    val questionCount = profile["question_count"]?.jsonPrimitive?.intOrNull
                    val answerCount = profile["answer_count"]?.jsonPrimitive?.intOrNull
                    if (questionCount != null && answerCount != null) {
                        QuestionAuthorActivityStats(questionCount, answerCount)
                    } else {
                        null
                    }
                }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("FeedBlocking", "Failed to load question author activity stats", error)
            null
        } finally {
            isQuestionAuthorStatsLoading = false
        }
    }

    FeedAuthorBlockConfirmDialogContent(
        request = request,
        displayItems = displayItems,
        questionAuthorStats = questionAuthorStats,
        isQuestionAuthorStatsLoading = isQuestionAuthorStatsLoading,
        onDismiss = onDismiss,
        onConfirmBlock = { author ->
            coroutineScope.launch {
                try {
                    when (request?.type) {
                        FeedAuthorBlockType.CONTENT_AUTHOR -> database.blockedUserDao().insertUser(
                            BlockedUser(
                                userId = author.id,
                                userName = author.name,
                                urlToken = author.urlToken,
                                avatarUrl = author.avatarUrl,
                            ),
                        )

                        FeedAuthorBlockType.QUESTION_AUTHOR -> database.blockedQuestionAuthorDao().insertUser(
                            BlockedQuestionAuthor(
                                userId = author.id,
                                userName = author.name,
                                urlToken = author.urlToken,
                                avatarUrl = author.avatarUrl,
                            ),
                        )

                        null -> return@launch
                    }
                    onConfirm()
                    val targetName = if (request.type == FeedAuthorBlockType.QUESTION_AUTHOR) "提问者" else "用户"
                    userMessages.showShortMessage("已屏蔽$targetName：${author.name}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("FeedBlocking", "Failed to block feed author", e)
                    userMessages.showShortMessage("屏蔽失败: ${e.message}")
                }
            }
        },
    )
}

@Composable
fun BlockByKeywordsDialog(
    showDialog: Boolean,
    feedTitle: String,
    feedExcerpt: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val database = koinInject<ContentFilterDatabase>()

    var extractedKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var keywordInfoList by remember { mutableStateOf<List<KeywordWithWeight>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog, feedTitle, feedExcerpt) {
        if (showDialog) {
            isLoading = true
            try {
                val keywordsWithWeight = extractFeedKeywords(feedTitle, feedExcerpt)
                keywordInfoList = keywordsWithWeight
                extractedKeywords = keywordsWithWeight.take(8).map { it.keyword }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FeedBlocking", "Failed to extract block keywords", e)
                userMessages.showShortMessage("提取关键词失败: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    BlockByKeywordsDialogContent(
        showDialog = showDialog,
        feedTitle = feedTitle,
        feedExcerpt = feedExcerpt,
        extractedKeywords = extractedKeywords,
        keywordInfoList = keywordInfoList,
        isLoading = isLoading,
        isAdding = isAdding,
        onDismiss = onDismiss,
        onConfirmPhrase = { phrase ->
            isAdding = true
            coroutineScope.launch {
                try {
                    database.blockedKeywordDao().insertKeyword(
                        BlockedKeyword(
                            keyword = phrase.trim(),
                            keywordType = KeywordType.NLP_SEMANTIC.name,
                        ),
                    )
                    userMessages.showShortMessage("已添加NLP屏蔽短语: $phrase")
                    onConfirm()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("FeedBlocking", "Failed to add NLP block phrase", e)
                    userMessages.showShortMessage("添加失败: ${e.message}")
                } finally {
                    isAdding = false
                }
            }
        },
    )
}
