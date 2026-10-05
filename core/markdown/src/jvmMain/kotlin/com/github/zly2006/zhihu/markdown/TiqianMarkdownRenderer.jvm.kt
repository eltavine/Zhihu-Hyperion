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

package com.github.zly2006.zhihu.markdown

import org.tiqian.shaping.skia.SkiaSystemTypefaces

/**
 * 提椠的桌面后端只在固定的候选字体族里找系统字体（中文是思源黑体、Noto Sans CJK SC、苹方等，拉丁文是 Inter、SF Pro、
 * Roboto、Noto Sans、Helvetica Neue），Windows 默认一个都没有，没装 Noto 或思源字体的 Linux 也一样。找不到时它用
 * 不含任何字形的空 typeface 测量和绘制，正文整段不可见，候选列表见
 * [SkiaSystemTypefaces](https://github.com/tiqian-cjk/tiqian/blob/7aa7c3d8ada486aecbdfceb0ed461fe775a213d9/platforms/jvm/skia/src/jvmMain/kotlin/org/tiqian/shaping/skia/SkiaTextBlobs.kt)。
 * 两类字体都解析得到时才提供提椠，否则正文回落到默认渲染器。
 */
actual val isTiqianMarkdownRendererAvailable: Boolean =
    SkiaSystemTypefaces.cjk != null && SkiaSystemTypefaces.latin != null
