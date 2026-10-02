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

package com.github.zly2006.zhihu.viewmodel

import android.content.ClipData
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.data.ZhihuJson.json
import com.github.zly2006.zhihu.icons.AppIcons
import com.github.zly2006.zhihu.navigation.requestLoginNavigation
import com.github.zly2006.zhihu.platform.androidUserMessageSink
import com.github.zly2006.zhihu.ui.components.AppDialogAction
import com.github.zly2006.zhihu.ui.components.AppDialogQueue
import com.github.zly2006.zhihu.ui.components.AppDialogRequest
import com.github.zly2006.zhihu.util.HttpStatusException
import com.github.zly2006.zhihu.util.clipboardManager
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.compose.koinInject
import org.koin.mp.KoinPlatform

/** 登录过期与接口错误交给 [dialogs] 排队显示；后台服务没有界面，传 null 时只用 Toast 提示。 */
class AndroidFetchFailurePresenter(
    private val context: Context,
    private val dialogs: AppDialogQueue?,
) : FetchFailurePresenter {
    private val userMessageSink by lazy { androidUserMessageSink(context) }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        if (error is HttpStatusException) {
            Log.e(tag, "Response: ${error.bodyText}", error)
            if (error.isLoginExpired()) {
                dialogs?.show(loginExpiredDialog())
                return
            }
            dialogs?.show(httpErrorDialog(error))
        }
        Log.e(tag, "Failed to fetch feeds", error)
        userMessageSink.showShortMessage("加载失败: ${error.message}")
    }

    override fun showFailureMessage(message: String) = userMessageSink.showShortMessage(message)

    private fun loginExpiredDialog() = AppDialogRequest(
        key = "login-expired",
        title = "登录已过期",
        text = "请重新登录以继续使用完整功能。",
        icon = AppIcons.Login,
        confirm = AppDialogAction("重新登录") {
            KoinPlatform.getKoin().get<ZhihuAccountStore>().clear()
            requestLoginNavigation()
        },
        dismiss = AppDialogAction("取消"),
    )

    private fun httpErrorDialog(error: HttpStatusException) = AppDialogRequest(
        key = "http-error",
        title = "请求失败（${error.status}）",
        text = error.bodyText,
        icon = AppIcons.Error,
        confirm = AppDialogAction("复制 curl") {
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("curl", error.dumpedCurlRequest))
            userMessageSink.showShortMessage("已复制到剪贴板")
        },
        dismiss = AppDialogAction("关闭"),
    )
}

private fun HttpStatusException.isLoginExpired(): Boolean = try {
    val errorBody = json.parseToJsonElement(bodyText).jsonObject["error"]?.jsonObject
    errorBody?.get("code")?.jsonPrimitive?.int == 100 &&
        errorBody["message"]?.jsonPrimitive?.content == "ERR_TICKET_NOT_EXIST"
} catch (_: Exception) {
    false
}

@Composable
actual fun rememberFetchFailurePresenter(): FetchFailurePresenter {
    val context = LocalContext.current
    val dialogs = koinInject<AppDialogQueue>()
    return remember(context, dialogs) { AndroidFetchFailurePresenter(context, dialogs) }
}
