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

package com.github.zly2006.zhihu.util

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import com.github.zly2006.zhihu.data.SegmentInfoMark
import com.github.zly2006.zhihu.data.SegmentInfoMeta
import com.github.zly2006.zhihu.data.SegmentInfoParagraph
import com.github.zly2006.zhihu.data.effectiveSegInfo

data class SegmentHighlightSpan(
    val text: String,
    val meta: SegmentInfoMeta,
    val displayText: String = text,
    val sourceUrl: String? = null,
    val contentId: String? = null,
    val contentType: String? = null,
    val paragraphId: String? = null,
    val startOffset: Int? = null,
    val endOffset: Int? = null,
)

data class SegmentTextPart(
    val text: String,
    val highlight: SegmentHighlightSpan? = null,
)

private data class NormalizedSegmentMark(
    val startIndex: Int,
    val endIndex: Int,
    val meta: SegmentInfoMeta,
    val isMaster: Boolean,
)

/**
 * 只有处在这些纯排版元素之内（或直接位于段落中）的文字会被包进划线；链接、脚注、公式等元素内部保持原样。
 * Markdown 转换也只把内容仅含这些元素的划线识别成段评，二者必须一致。
 */
internal val segmentHighlightFormatTags = setOf("b", "strong", "i", "em")

private class PositionedText(
    val node: TextNode,
    val start: Int,
) {
    val end: Int
        get() = start + node.getWholeText().length
}

private class PreparedParagraph(
    val pid: String,
    val text: String,
    val parts: List<SegmentTextPart>,
    val injectableTexts: List<PositionedText>,
)

/**
 * `segment_infos` 的偏移针对段落纯文本：只有全部文字节点依次拼起来恰好等于 [segmentText] 时，偏移才能对应到 DOM，
 * 否则返回 null，整段保持原样（例如 `<br>` 在 `Element.text()` 里会变成空格，但不是文字节点）。
 */
private fun Element.injectableTexts(segmentText: String): List<PositionedText>? {
    val texts = StringBuilder()
    val injectable = mutableListOf<PositionedText>()

    fun collect(node: Node, formattingOnly: Boolean) {
        when (node) {
            is TextNode -> {
                if (formattingOnly) injectable += PositionedText(node, texts.length)
                texts.append(node.getWholeText())
            }

            is Element -> {
                val childFormattingOnly = formattingOnly && node.tagName().lowercase() in segmentHighlightFormatTags
                node.childNodes().forEach { child -> collect(child, childFormattingOnly) }
            }
        }
    }
    childNodes().forEach { child -> collect(child, formattingOnly = true) }
    return injectable.takeIf { texts.toString() == segmentText }
}

fun buildSegmentTextParts(
    text: String,
    marks: List<SegmentInfoMark>,
    sourceUrl: String? = null,
    contentId: String? = null,
    contentType: String? = null,
    paragraphId: String? = null,
): List<SegmentTextPart> {
    if (text.isEmpty()) return emptyList()
    if (marks.isEmpty()) return listOf(SegmentTextPart(text))

    val normalized = marks
        .mapNotNull { mark ->
            val start = mark.startIndex.coerceIn(0, text.length)
            val end = mark.endIndex.coerceIn(start, text.length)
            val meta = mark.effectiveSegInfo
            if (start >= end || meta == null) {
                null
            } else {
                NormalizedSegmentMark(
                    startIndex = start,
                    endIndex = end,
                    meta = meta,
                    isMaster = mark.masterSegInfo != null,
                )
            }
        }.groupBy { it.startIndex to it.endIndex }
        .map { (_, sameRangeMarks) ->
            sameRangeMarks.mergeSameRangeMarks()
        }.sortedWith(compareBy<NormalizedSegmentMark> { it.startIndex }.thenBy { it.endIndex })
    if (normalized.isEmpty()) return listOf(SegmentTextPart(text))

    val parts = mutableListOf<SegmentTextPart>()
    var cursor = 0
    normalized.forEach { mark ->
        if (mark.startIndex < cursor) return@forEach
        if (mark.startIndex > cursor) {
            parts += SegmentTextPart(text.substring(cursor, mark.startIndex))
        }
        parts += SegmentTextPart(
            text = text.substring(mark.startIndex, mark.endIndex),
            highlight = SegmentHighlightSpan(
                text = text.substring(mark.startIndex, mark.endIndex),
                meta = mark.meta,
                sourceUrl = sourceUrl,
                contentId = contentId,
                contentType = contentType,
                paragraphId = paragraphId,
                startOffset = mark.startIndex,
                endOffset = mark.endIndex,
            ),
        )
        cursor = mark.endIndex
    }
    if (cursor < text.length) {
        parts += SegmentTextPart(text.substring(cursor))
    }
    return parts.filter { it.text.isNotEmpty() }
}

private fun List<NormalizedSegmentMark>.mergeSameRangeMarks(): NormalizedSegmentMark {
    val masterMarks = filter { it.isMaster }
    val ordered = masterMarks + filterNot { it.isMaster }
    val mergedMeta = SegmentInfoMeta(
        segIds = ordered
            .flatMap { it.meta.segIds }
            .distinct(),
        isLike = any { it.meta.isLike },
        likeCount = maxOf { it.meta.likeCount },
        commentCount = maxOf { it.meta.commentCount },
        myCommentCount = maxOf { it.meta.myCommentCount },
        isSpan = any { it.meta.isSpan },
    )
    val first = first()
    return first.copy(
        meta = mergedMeta,
        isMaster = masterMarks.isNotEmpty(),
    )
}

fun applySegmentInfosToHtml(
    content: String,
    segmentInfos: List<SegmentInfoParagraph>,
    sourceUrl: String? = null,
    contentId: String? = null,
    contentType: String? = null,
): String {
    if (content.isBlank() || segmentInfos.isEmpty()) return content

    val document = Ksoup.parseBodyFragment(content)
    val segmentInfosByPid = segmentInfos.associateBy(SegmentInfoParagraph::pid)
    val preparedParagraphs = document
        .select("p[data-pid]")
        .mapNotNull { target ->
            val paragraph = segmentInfosByPid[target.attr("data-pid")] ?: return@mapNotNull null
            PreparedParagraph(
                pid = paragraph.pid,
                text = paragraph.text,
                parts = buildSegmentTextParts(
                    text = paragraph.text,
                    marks = paragraph.marks,
                    sourceUrl = sourceUrl,
                    contentId = contentId,
                    contentType = contentType,
                    paragraphId = paragraph.pid,
                ),
                injectableTexts = target.injectableTexts(paragraph.text) ?: return@mapNotNull null,
            )
        }
    val spanFragments = preparedParagraphs.flatMap { paragraph ->
        paragraph.parts.mapNotNull { part ->
            val highlight = part.highlight?.takeIf { it.meta.isSpan } ?: return@mapNotNull null
            val segmentId = highlight.meta.segIds
                .joinToString(",")
                .takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            segmentId to (paragraph.pid to part.text)
        }
    }
    val spanDisplayTexts = mutableMapOf<String, String>()
    spanFragments
        .groupBy({ it.first }, { it.second })
        .forEach { (segmentId, fragments) ->
            spanDisplayTexts[segmentId] = buildString {
                var previousParagraphId: String? = null
                fragments.forEach { (paragraphId, text) ->
                    if (isNotEmpty() && paragraphId != previousParagraphId) append("\n\n")
                    append(text)
                    previousParagraphId = paragraphId
                }
            }
        }
    val displayTextOwners = spanDisplayTexts.keys.toMutableSet()

    preparedParagraphs.forEach { paragraph ->
        val boundaries = paragraph.parts.runningFold(0) { offset, part -> offset + part.text.length }
        val pieces = paragraph.injectableTexts.flatMap { text ->
            val cuts = boundaries.filter { it > text.start && it < text.end }.sortedDescending()
            listOf(text) + cuts.map { cut -> PositionedText(text.node.splitText(cut - text.start), cut) }.reversed()
        }
        paragraph.parts.forEachIndexed { index, part ->
            val highlight = part.highlight ?: return@forEachIndexed
            val fragments = pieces.filter { it.start >= boundaries[index] && it.end <= boundaries[index + 1] }
            if (fragments.isEmpty()) return@forEachIndexed
            val segmentId = highlight.meta.segIds.joinToString(",")
            val displayText = when {
                !highlight.meta.isSpan -> part.text
                displayTextOwners.remove(segmentId) -> spanDisplayTexts[segmentId]
                else -> null
            }
            fragments.forEachIndexed { fragmentIndex, fragment ->
                val span = Element("span").apply {
                    addClass("highlight-wrap")
                    addClass("other")
                    if (highlight.meta.commentCount > 0) {
                        addClass("has-comments")
                    }
                    attr("data-highlight-id", segmentId)
                    attr("data-highlight-like-count", highlight.meta.likeCount.toString())
                    attr("data-highlight-comment-count", highlight.meta.commentCount.toString())
                    attr("data-highlight-my-comment-count", highlight.meta.myCommentCount.toString())
                    attr("data-highlight-is-like", highlight.meta.isLike.toString())
                    attr("data-highlight-is-span", highlight.meta.isSpan.toString())
                    displayText
                        ?.takeIf { fragmentIndex == 0 && it != fragment.node.getWholeText() }
                        ?.let { attr("data-highlight-display-text", it) }
                    attr(
                        "data-highlight-split-type",
                        when {
                            part.text == paragraph.text -> "both"
                            paragraph.text.startsWith(part.text) -> "head"
                            paragraph.text.endsWith(part.text) -> "tail"
                            else -> "middle"
                        },
                    )
                    attr("data-highlight-id-extra", "")
                    highlight.sourceUrl?.let { attr("data-highlight-source-url", it) }
                    contentId?.let { attr("data-highlight-content-id", it) }
                    contentType?.let { attr("data-highlight-content-type", it) }
                    highlight.paragraphId?.let { attr("data-highlight-pid", it) }
                    highlight.startOffset?.let { attr("data-highlight-start-offset", it.toString()) }
                    highlight.endOffset?.let { attr("data-highlight-end-offset", it.toString()) }
                }
                fragment.node.replaceWith(span)
                span.appendChild(fragment.node)
            }
        }
    }
    return document.body().html()
}
