@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.hrm.markdown.renderer.selection

import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.SelectionLayoutBuilder

internal actual fun Selectable.appendSelectableInfoCompat(builder: SelectionLayoutBuilder, isLast: Boolean) {
    appendSelectableInfoToBuilder(builder, isLast)
}

internal actual fun forwardingPersistentSelectable(delegate: Selectable, documentOrder: List<Int>): PersistentSelectable =
    object : PersistentSelectable(delegate, documentOrder) {
        override fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder, isLast: Boolean) {
            currentDelegate?.appendSelectableInfoToBuilder(builder, isLast)
        }
    }
