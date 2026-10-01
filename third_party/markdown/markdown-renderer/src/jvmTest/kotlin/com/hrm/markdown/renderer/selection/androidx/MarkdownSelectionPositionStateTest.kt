@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.hrm.markdown.renderer.selection.androidx

import androidx.collection.mutableLongObjectMapOf
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionLayoutBuilder
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.hrm.markdown.renderer.selection.DocumentOrderedSelectable
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownSelectionPositionStateTest {
    @Test
    fun movingSelectedTextReRunsDerivedCoordinateReaders() {
        val registrar = MarkdownSelectionRegistrarImpl()
        val selected = registrar.subscribeText(documentOrder = listOf(0))
        val unselected = registrar.subscribeText(documentOrder = listOf(1))
        registrar.subselections = mutableLongObjectMapOf(
            selected,
            Selection(
                start = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 0, selected),
                end = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 1, selected),
            ),
        )
        var observingEvaluations = 0
        val observing = derivedStateOf {
            observingEvaluations++
            registrar.sort().map { it.observeLayoutCoordinates() }
        }
        var plainEvaluations = 0
        val plain = derivedStateOf {
            plainEvaluations++
            registrar.sort().map { it.getLayoutCoordinates() }
        }

        observing.value
        plain.value
        registrar.notifyPositionChange(unselected)
        Snapshot.sendApplyNotifications()
        observing.value
        assertEquals(1, observingEvaluations)

        registrar.notifyPositionChange(selected)
        Snapshot.sendApplyNotifications()
        observing.value
        plain.value
        assertEquals(2, observingEvaluations)
        assertEquals(1, plainEvaluations)
    }
}

private fun MarkdownSelectionRegistrarImpl.subscribeText(documentOrder: List<Int>): Long {
    val id = nextSelectableId()
    subscribe(
        object : Selectable, DocumentOrderedSelectable {
            override val selectableId = id
            override val documentOrder = documentOrder
            override val pinnableContainer: PinnableContainer? = null
            override val bringIntoViewRequester: BringIntoViewRequester? = null

            override fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder, isLast: Boolean) = Unit

            override fun getSelectAllSelection(): Selection? = null

            override fun getHandlePosition(selection: Selection, isStartHandle: Boolean) = Offset.Zero

            override fun getLayoutCoordinates(): LayoutCoordinates? = null

            override fun textLayoutResult(): TextLayoutResult? = null

            override fun getText() = AnnotatedString("text")

            override fun getBoundingBox(offset: Int) = Rect.Zero

            override fun getLineLeft(offset: Int) = 0f

            override fun getLineRight(offset: Int) = 0f

            override fun getCenterYForOffset(offset: Int) = 0f

            override fun getRangeOfLineContaining(offset: Int) = TextRange.Zero

            override fun getLastVisibleOffset() = 0

            override fun getLineHeight(offset: Int) = 0f
        },
    )
    return id
}
