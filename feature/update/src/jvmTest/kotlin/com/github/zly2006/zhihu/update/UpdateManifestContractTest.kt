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

package com.github.zly2006.zhihu.update

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `resources/update.json` is what CI's `write_update_manifest.py` produces for fixed inputs;
 * `.github/scripts/test_write_update_manifest.py` fails when the script's output drifts from it.
 */
class UpdateManifestContractTest {
    @Test
    fun ciManifestDecodesWithAPackageForEveryTarget() {
        val manifest = manifestJson.decodeFromString<UpdateManifest>(
            checkNotNull(javaClass.getResource("/update.json")).readText(),
        )

        assertEquals(1300, manifest.versionCode)
        assertEquals("https://github.com/eltavine/Zhihu-Hyperion/releases/tag/nightly", manifest.releaseUrl)
        assertEquals(UpdateTarget.entries.map { it.key }.toSet(), manifest.assets.keys)
        assertEquals(
            "https://github.com/eltavine/Zhihu-Hyperion/releases/download/nightly/zhihu-hyperion-lite.apk",
            manifest.assets.getValue(UpdateTarget.ANDROID_LITE.key).url,
        )
    }
}
