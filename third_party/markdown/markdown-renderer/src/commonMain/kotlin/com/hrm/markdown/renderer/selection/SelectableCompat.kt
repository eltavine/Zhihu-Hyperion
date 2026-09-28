@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.hrm.markdown.renderer.selection

import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.SelectionLayoutBuilder

// Compose Multiplatform 1.12 split the internal Selectable API: the JetBrains foundation fork used on desktop,
// macOS and iOS passes `isLast` to `appendSelectableInfoToBuilder`, while AndroidX foundation does not. Only
// these two declarations have per-platform actuals (androidMain and skikoMain); the selection code stays shared.

/** Forwards to [Selectable.appendSelectableInfoToBuilder]; AndroidX ignores [isLast]. */
internal expect fun Selectable.appendSelectableInfoCompat(builder: SelectionLayoutBuilder, isLast: Boolean)

/** Creates a [PersistentSelectable] that forwards `appendSelectableInfoToBuilder` with the platform signature. */
internal expect fun forwardingPersistentSelectable(delegate: Selectable, documentOrder: List<Int>): PersistentSelectable
