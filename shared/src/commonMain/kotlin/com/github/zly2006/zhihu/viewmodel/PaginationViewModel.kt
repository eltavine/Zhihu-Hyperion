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

package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.ARTICLE_FOLLOWERS_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.ARTICLE_VOTEUP_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.FeedDisplaySettings
import com.github.zly2006.zhihu.data.OnlineHistoryDeletePair
import com.github.zly2006.zhihu.data.QUALITY_FILTER_MODE_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.QUESTION_ANSWER_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.QUESTION_FOLLOWERS_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.QualityFilterMode
import com.github.zly2006.zhihu.data.QualityFilterSettings
import com.github.zly2006.zhihu.data.VIDEO_FOLLOWERS_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.VIDEO_VOTE_THRESHOLD_PREFERENCE_KEY
import com.github.zly2006.zhihu.data.ZhihuJson.decodeJson
import com.github.zly2006.zhihu.data.ZhihuPaging
import com.github.zly2006.zhihu.data.fetchZhihuContentDetail
import com.github.zly2006.zhihu.data.getOrFetchContentDetail
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.platform.isFeedQualityFilterSupported
import com.github.zly2006.zhihu.util.Log
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import kotlin.reflect.KType

abstract class PaginationViewModel<T : Any>(
    val dataType: KType,
) : ViewModel() {
    val allData = mutableStateListOf<T>()
    val debugData: MutableList<JsonElement> = mutableListOf()
    var isLoading: Boolean by mutableStateOf(false)
        protected set
    var errorMessage: String? = null
        protected set
    var allowGuestAccess = false
    protected var lastPaging: ZhihuPaging? by mutableStateOf(null)
    open val isEnd: Boolean get() = lastPaging?.isEnd == true
    protected abstract val initialUrl: String
    private var currentJob: Job? = null
    protected open val shouldLogDecodeFailures: Boolean = true

    protected open fun resolvePageUrl(): String = lastPaging?.next ?: initialUrl

    /**
     * Generally used fields to include in the API request.
     * This can be overridden in subclasses to include more specific fields.
     */
    open val include = "data[*].content,excerpt,headline,target.author.badge_v2"

    open fun refresh(environment: ZhihuApiEnvironment) {
        currentJob?.cancel()
        currentJob = null
        isLoading = false
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        loadMore(environment)
    }

    protected open fun handlePageMetadata(json: JsonObject) = Unit

    protected open suspend fun processResponse(environment: ZhihuApiEnvironment, data: List<T>, rawData: JsonArray) {
        debugData.addAll(rawData) // 保存原始JSON
        allData.addAll(data) // 保存未flatten的数据
    }

    protected open fun decodePage(
        environment: ZhihuApiEnvironment,
        rawData: JsonArray,
    ): List<T> = rawData.mapNotNull {
        if ("type" in it.jsonObject &&
            it.jsonObject["type"]?.jsonPrimitive?.content in listOf(
                "invited_answer",
                "tab_list",
                "feed_item_index_group",
            )
        ) {
            return@mapNotNull null
        }
        try {
            @Suppress("UNCHECKED_CAST")
            decodeJson(serializer(dataType) as KSerializer<T>, it)
        } catch (e: Exception) {
            if (shouldLogDecodeFailures) {
                environment.logDecodeFailure(this::class.simpleName, it, e)
            }
            null
        }
    }

    protected open suspend fun fetchFeeds(environment: ZhihuApiEnvironment) {
        try {
            val url = resolvePageUrl()

            @Suppress("HttpUrlsUsage")
            val json = environment.fetchJson(url.replace("http://", "https://"), include)
                ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: not json object."))

            val jsonArray = json["data"] as? JsonArray
                ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: no $.data"))
            processResponse(environment, decodePage(environment, jsonArray), jsonArray)
            if ("paging" in json) {
                lastPaging = decodeJson(json["paging"]!!)
            }
            handlePageMetadata(json)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: NoTransformationFoundException) {
            throw RuntimeException("您可能已被风控，请重新登录。")
        } catch (e: Exception) {
            environment.handleFetchFailure(this::class.simpleName, e)
            errorHandle(e)
        } finally {
            isLoading = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    open fun loadMore(environment: ZhihuApiEnvironment) {
        if (isLoading || isEnd) return // 使用新的isEnd getter
        isLoading = true
        currentJob = viewModelScope.launch {
            try {
                fetchFeeds(environment)
            } catch (e: Exception) {
                errorHandle(e)
            }
        }
    }

    protected fun errorHandle(e: Exception) {
        if (e !is CancellationException) {
            errorMessage = e.message
            isLoading = false
        }
    }
}

interface PreparedArticleExportContent

interface ArticleImageExportRenderer {
    suspend fun prepareExportWebView(htmlContent: String, timeoutMs: Long): PreparedArticleExportContent

    suspend fun captureExportBitmap(preparedWebView: PreparedArticleExportContent): Any

    suspend fun destroyExportWebView(preparedWebView: PreparedArticleExportContent)

    fun recycleExportBitmap(bitmap: Any)
}

suspend fun ZhihuApiEnvironment.fetchContentDetail(destination: NavDestination): DataHolder.Content? =
    runCatching {
        fetchZhihuContentDetail(destination) { url, include ->
            fetchJson(url, include)
        }
    }.getOrElse { error ->
        if (error is CancellationException) throw error
        Log.e("ZhihuApiEnvironment", "Failed to fetch content detail for $destination", error)
        null
    }

suspend fun ZhihuApiEnvironment.getOrFetchContentDetail(destination: NavDestination): DataHolder.Content? =
    runCatching {
        ContentDetailCache.getOrFetchContentDetail(destination) { url, include ->
            fetchJson(url, include)
        }
    }.getOrElse { error ->
        if (error is CancellationException) throw error
        Log.e("ZhihuApiEnvironment", "Failed to fetch content detail for $destination", error)
        null
    }

suspend fun ZhihuApiEnvironment.addReadHistory(
    contentToken: String,
    contentTypeName: String,
) {
    if (authenticatedCookies()["d_c0"] == null) return
    runCatching {
        postSigned("https://www.zhihu.com/api/v4/read_history/add") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("content_token", contentToken)
                    put("content_type", contentTypeName)
                }.toString(),
            )
        }
    }
}

/** 删除在线浏览历史；[clear] 为 true 时服务端清空当前账号的全部记录，此时 [pairs] 为空。 */
internal suspend fun ZhihuApiEnvironment.deleteOnlineHistory(
    pairs: List<OnlineHistoryDeletePair>,
    clear: Boolean,
): HttpResponse = postSigned("https://api.zhihu.com/read_history/batch_del") {
    contentType(ContentType.Application.Json)
    setBody(
        buildJsonObject {
            put(
                "pairs",
                JsonArray(
                    pairs.map { pair ->
                        buildJsonObject {
                            put("content_token", pair.contentToken)
                            put("content_type", pair.contentType)
                        }
                    },
                ),
            )
            put("clear", clear)
        }.toString(),
    )
}

/** 列表卡片的质量屏蔽与反向屏蔽展示方式；未支持质量屏蔽的平台始终按关闭处理。 */
fun SettingsStore.toFeedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings(
    qualityFilterMode = if (isFeedQualityFilterSupported) {
        QualityFilterMode.entries.firstOrNull {
            it.name == getString(QUALITY_FILTER_MODE_PREFERENCE_KEY, QualityFilterMode.RULES.name)
        } ?: QualityFilterMode.RULES
    } else {
        QualityFilterMode.OFF
    },
    qualityFilter = QualityFilterSettings(
        answerVoteupCount = getInt(ANSWER_VOTEUP_THRESHOLD_PREFERENCE_KEY, 10).coerceAtLeast(0),
        articleVoteupCount = getInt(ARTICLE_VOTEUP_THRESHOLD_PREFERENCE_KEY, 20).coerceAtLeast(0),
        articleFollowersCount = getInt(ARTICLE_FOLLOWERS_THRESHOLD_PREFERENCE_KEY, 50).coerceAtLeast(0),
        videoVoteCount = getInt(VIDEO_VOTE_THRESHOLD_PREFERENCE_KEY, 20).coerceAtLeast(0),
        videoFollowersCount = getInt(VIDEO_FOLLOWERS_THRESHOLD_PREFERENCE_KEY, 50).coerceAtLeast(0),
        questionAnswerCount = getInt(QUESTION_ANSWER_THRESHOLD_PREFERENCE_KEY, 0).coerceAtLeast(0),
        questionFollowersCount = getInt(QUESTION_FOLLOWERS_THRESHOLD_PREFERENCE_KEY, 50).coerceAtLeast(0),
    ),
    reverseBlock = getBoolean("reverseBlock", false),
)

data class HomeFeedFilterResult(
    val foregroundItems: List<FeedDisplayItem>,
    val filteredItems: List<FeedDisplayItem>,
    val reverseBlock: Boolean,
)
