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

import com.github.zly2006.zhihu.buildlogic.selectedAndroidVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Android Full/Lite `actual`s for a Kotlin Multiplatform module; apply after `zhihu.kmp.library`.
 *
 * `src/androidFull` and `src/androidLite` are both source sets so ktlint checks them, but only the one matching
 * [selectedAndroidVariant] joins the Android compilation; see `docs/kmp-android-variants.md`.
 */
class KmpAndroidVariantsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val selectedVariant = selectedAndroidVariant
            extensions.configure<KotlinMultiplatformExtension> {
                // Explicit dependsOn edges switch off the implicit default hierarchy (nativeMain, appleMain, ...).
                applyDefaultHierarchyTemplate()
                val commonMain = sourceSets.getByName("commonMain")
                listOf("Full", "Lite").forEach { variant ->
                    val variantMain = sourceSets.create("android$variant") { dependsOn(commonMain) }
                    if (variant == selectedVariant) {
                        sourceSets.getByName("androidMain").dependsOn(variantMain)
                    }
                }
            }
        }
    }
}
