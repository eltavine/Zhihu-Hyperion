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

plugins {
    `kotlin-dsl`
}

group = "com.github.zly2006.zhihu.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.multiplatform.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.module.graph.assertion.gradlePlugin)
    compileOnly(libs.aboutlibraries.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("ktlint") {
            id = "zhihu.ktlint"
            implementationClass = "KtlintConventionPlugin"
        }
        register("kmpLibrary") {
            id = "zhihu.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "zhihu.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpAndroidVariants") {
            id = "zhihu.kmp.android.variants"
            implementationClass = "KmpAndroidVariantsConventionPlugin"
        }
        register("androidApplication") {
            id = "zhihu.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("room") {
            id = "zhihu.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("macosApp") {
            id = "zhihu.macos.app"
            implementationClass = "MacosAppConventionPlugin"
        }
        register("moduleGraph") {
            id = "zhihu.module.graph"
            implementationClass = "ModuleGraphConventionPlugin"
        }
        register("aboutLibraries") {
            id = "zhihu.aboutlibraries"
            implementationClass = "AboutLibrariesConventionPlugin"
        }
    }
}
