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

// Plugin versions live in gradle/libs.versions.toml; modules apply them through build-logic convention plugins.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.module.graph.assertion) apply false
}

// Single test entry point for CI: new :core or :feature modules are covered without editing workflows.
tasks.register("jvmUnitTests") {
    group = "verification"
    description = "Runs the JVM tests of :shared and every :core/:feature module, plus the app's unit tests."
    dependsOn(
        subprojects
            .filter { it.path == ":shared" || it.path.startsWith(":core:") || it.path.startsWith(":feature:") }
            .map { "${it.path}:jvmTest" },
    )
    dependsOn(":app:testLiteDebugUnitTest")
}
