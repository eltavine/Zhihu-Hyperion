@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)

package com.hrm.markdown.renderer.selection

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentSelectionKeyboardTest {
    @Test
    fun platformShortcutsSelectEveryParagraphAndCopyIt() = runComposeUiTest {
        val clipboard = RecordingClipboard()
        setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                PersistentSelectionContainer(modifier = Modifier.testTag(SELECTION_TAG)) {
                    PersistentSelectionScope(scopeKey = 0, documentOrder = listOf(0)) { BasicText("第一段") }
                    PersistentSelectionScope(scopeKey = 1, documentOrder = listOf(1)) { BasicText("第二段") }
                }
            }
        }
        val shortcut = if (DesktopPlatform.Current == DesktopPlatform.MacOS) Key.MetaLeft else Key.CtrlLeft

        onNodeWithTag(SELECTION_TAG).requestFocus()
        onNodeWithTag(SELECTION_TAG).performKeyInput {
            withKeyDown(shortcut) {
                pressKey(Key.A)
                pressKey(Key.C)
            }
        }
        waitForIdle()

        assertEquals("第一段\n第二段", clipboard.text)
    }
}

private const val SELECTION_TAG = "selection"

private class RecordingClipboard : Clipboard {
    private var entry: ClipEntry? = null

    val text: String?
        get() = (entry?.nativeClipEntry as? Transferable)?.getTransferData(DataFlavor.stringFlavor) as? String

    override suspend fun getClipEntry(): ClipEntry? = entry

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        entry = clipEntry
    }
}
