@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hrm.markdown.renderer.selection.androidx

import androidx.collection.LongObjectMap
import androidx.collection.emptyLongObjectMap
import androidx.collection.mutableLongObjectMapOf
import androidx.compose.foundation.AtomicLong
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionLayoutBuilder
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.hrm.markdown.renderer.selection.DocumentOrderedSelectable
import com.hrm.markdown.renderer.selection.appendSelectableInfoCompat
import kotlin.math.min

/**
 * A selectable whose linear order is supplied by its document model rather than its layout.
 *
 * Deliberately not a foundation [Selectable]: that internal interface has platform-specific signatures, while this
 * one is only consumed by [MarkdownSelectionManager] and stays identical on every platform.
 */
internal interface MarkdownSelectable : DocumentOrderedSelectable {
    val selectableId: Long
    fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder, isLast: Boolean)
    fun getSelectAllSelection(): Selection?
    fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset
    fun getLayoutCoordinates(): LayoutCoordinates?

    /**
     * [getLayoutCoordinates] for snapshot readers such as a `derivedStateOf`, which run again after
     * [notifyPositionChange]. It is not the default accessor because the manager also reads coordinates while
     * its container composes; subscribing that composition recomposed it on every position change and the
     * container never became idle.
     */
    fun observeLayoutCoordinates(): LayoutCoordinates?

    fun notifyPositionChange()
    fun textLayoutResult(): TextLayoutResult?
    fun getText(): AnnotatedString
    fun getBoundingBox(offset: Int): Rect
    fun getLineLeft(offset: Int): Float
    fun getLineRight(offset: Int): Float
    fun getCenterYForOffset(offset: Int): Float
    fun getRangeOfLineContaining(offset: Int): TextRange
    fun getLastVisibleOffset(): Int
    fun getLineHeight(offset: Int): Float
}

private class MarkdownSelectableAdapter(
    private val selectable: Selectable,
    private val orderedSelectable: DocumentOrderedSelectable,
) : MarkdownSelectable {
    private var position by mutableStateOf(Unit, neverEqualPolicy())

    override val documentOrder: List<Int>
        get() = orderedSelectable.documentOrder
    override val selectableId: Long
        get() = selectable.selectableId
    override fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder, isLast: Boolean) =
        selectable.appendSelectableInfoCompat(builder, isLast)
    override fun getSelectAllSelection(): Selection? = selectable.getSelectAllSelection()
    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset =
        selectable.getHandlePosition(selection, isStartHandle)
    override fun getLayoutCoordinates(): LayoutCoordinates? = selectable.getLayoutCoordinates()
    override fun observeLayoutCoordinates(): LayoutCoordinates? {
        position
        return selectable.getLayoutCoordinates()
    }
    override fun notifyPositionChange() {
        position = Unit
    }
    override fun textLayoutResult(): TextLayoutResult? = selectable.textLayoutResult()
    override fun getText(): AnnotatedString = selectable.getText()
    override fun getBoundingBox(offset: Int): Rect = selectable.getBoundingBox(offset)
    override fun getLineLeft(offset: Int): Float = selectable.getLineLeft(offset)
    override fun getLineRight(offset: Int): Float = selectable.getLineRight(offset)
    override fun getCenterYForOffset(offset: Int): Float = selectable.getCenterYForOffset(offset)
    override fun getRangeOfLineContaining(offset: Int): TextRange = selectable.getRangeOfLineContaining(offset)
    override fun getLastVisibleOffset(): Int = selectable.getLastVisibleOffset()
    override fun getLineHeight(offset: Int): Float = selectable.getLineHeight(offset)
}

internal fun compareMarkdownDocumentOrder(first: List<Int>, second: List<Int>): Int {
    val sharedSize = min(first.size, second.size)
    for (index in 0 until sharedSize) {
        val comparison = first[index].compareTo(second[index])
        if (comparison != 0) return comparison
    }
    return first.size.compareTo(second.size)
}

internal class MarkdownSelectionRegistrarImpl private constructor(initialIncrementId: Long) :
    SelectionRegistrar {
    companion object {
        val Saver =
            Saver<MarkdownSelectionRegistrarImpl, Long>(
                save = { it.incrementId.get() },
                restore = { MarkdownSelectionRegistrarImpl(it) },
            )
    }

    constructor() : this(initialIncrementId = 1L)

    /** A flag to check if the [Selectable]s have already been sorted. */
    internal var sorted: Boolean = false

    /**
     * This is essentially the list of registered components that want to handle text selection that
     * are below the SelectionContainer.
     */
    private val _selectables = mutableListOf<MarkdownSelectable>()

    /** Getter for handlers that returns a List. */
    internal val selectables: List<MarkdownSelectable>
        get() = _selectables

    private val _selectableMap = mutableLongObjectMapOf<MarkdownSelectable>()

    /** A map from selectable keys to subscribed selectables. */
    internal val selectableMap: LongObjectMap<MarkdownSelectable>
        get() = _selectableMap

    /**
     * The incremental id to be assigned to each selectable. It starts from 1 and 0 is used to
     * denote an invalid id.
     *
     * @see SelectionRegistrar.InvalidSelectableId
     */
    private var incrementId = AtomicLong(initialIncrementId)

    /** The callback to be invoked when the position change was triggered. */
    internal var onPositionChangeCallback: ((Long) -> Unit)? = null

    /** The callback to be invoked when the selection is initiated. */
    internal var onSelectionUpdateStartCallback:
        ((Boolean, LayoutCoordinates, Offset, SelectionAdjustment) -> Unit)? =
        null

    /** The callback to be invoked when the selection is initiated with selectAll [Selection]. */
    internal var onSelectionUpdateSelectAll: ((Boolean, Long) -> Unit)? = null

    /**
     * The callback to be invoked when the selection is updated. If the first offset is null it
     * means that the start of selection is unknown for the caller.
     */
    internal var onSelectionUpdateCallback:
        ((Boolean, LayoutCoordinates, Offset, Offset, Boolean, SelectionAdjustment) -> Boolean)? =
        null

    /** The callback to be invoked when selection update finished. */
    internal var onSelectionUpdateEndCallback: (() -> Unit)? = null

    /** The callback to be invoked when one of the selectable has changed. */
    internal var onSelectableChangeCallback: ((Long) -> Unit)? = null

    /**
     * The callback to be invoked after a selectable is unsubscribed from this [SelectionRegistrar].
     */
    internal var afterSelectableUnsubscribe: ((Long) -> Unit)? = null

    override var subselections: LongObjectMap<Selection> by mutableStateOf(emptyLongObjectMap())

    override fun subscribe(selectable: Selectable): Selectable {
        val orderedSelectable = selectable as? DocumentOrderedSelectable
            ?: error("Markdown selection requires an explicit document order")
        val adapter = MarkdownSelectableAdapter(selectable, orderedSelectable)
        requirePrecondition(adapter.selectableId != SelectionRegistrar.InvalidSelectableId) {
            "The selectable contains an invalid id: ${adapter.selectableId}"
        }
        requirePrecondition(!_selectableMap.containsKey(adapter.selectableId)) {
            "Another selectable with the id: ${adapter.selectableId} has already subscribed."
        }
        _selectableMap[adapter.selectableId] = adapter
        _selectables.add(adapter)
        sorted = false
        return selectable
    }

    override fun unsubscribe(selectable: Selectable) {
        if (!_selectableMap.containsKey(selectable.selectableId)) return
        _selectableMap.remove(selectable.selectableId)?.let(_selectables::remove)
        afterSelectableUnsubscribe?.invoke(selectable.selectableId)
    }

    override fun nextSelectableId(): Long {
        var id = incrementId.getAndIncrement()
        while (id == SelectionRegistrar.InvalidSelectableId) {
            id = incrementId.getAndIncrement()
        }
        return id
    }

    /** Sorts every retained selectable by its stable position in the Markdown document. */
    fun sort(): List<MarkdownSelectable> {
        if (!sorted) {
            _selectables.sortWith { a, b ->
                val documentComparison = compareMarkdownDocumentOrder(a.documentOrder, b.documentOrder)
                check(documentComparison != 0 || a.selectableId == b.selectableId) {
                    "Duplicate Markdown document order: ${a.documentOrder}"
                }
                documentComparison
            }
            sorted = true
        }
        return selectables
    }

    override fun notifyPositionChange(selectableId: Long) {
        sorted = false
        // Coordinates are only read under snapshot observation for selected text, while scrolling moves every
        // visible selectable; writing state for unselected ones would invalidate nothing.
        if (selectableId in subselections) _selectableMap[selectableId]?.notifyPositionChange()
        onPositionChangeCallback?.invoke(selectableId)
    }

    override fun notifySelectionUpdateStart(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset,
        adjustment: SelectionAdjustment,
        isInTouchMode: Boolean,
    ) {
        onSelectionUpdateStartCallback?.invoke(
            isInTouchMode,
            layoutCoordinates,
            startPosition,
            adjustment,
        )
    }

    override fun notifySelectionUpdateSelectAll(selectableId: Long, isInTouchMode: Boolean) {
        onSelectionUpdateSelectAll?.invoke(isInTouchMode, selectableId)
    }

    override fun notifySelectionUpdate(
        layoutCoordinates: LayoutCoordinates,
        newPosition: Offset,
        previousPosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
        isInTouchMode: Boolean,
    ): Boolean {
        return onSelectionUpdateCallback?.invoke(
            isInTouchMode,
            layoutCoordinates,
            newPosition,
            previousPosition,
            isStartHandle,
            adjustment,
        ) ?: true
    }

    override fun notifySelectionUpdateEnd() {
        onSelectionUpdateEndCallback?.invoke()
    }

    override fun notifySelectableChange(selectableId: Long) {
        onSelectableChangeCallback?.invoke(selectableId)
    }
}
