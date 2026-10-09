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

import com.github.zly2006.zhihu.buildlogic.javafx

plugins {
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
    id("zhihu.kmp.android.variants")
}

kotlin {
    android {
        // The Android KMP library plugin only packages Compose resources (the author badge icon) into the APK's assets
        // when Android resources are enabled; without it AuthorBadge throws MissingResourceException at runtime.
        androidResources.enable = true
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.data)
            api(projects.core.database)
            api(projects.core.designsystem)
            api(projects.core.glass)
            api(projects.core.icons)
            api(projects.core.model)
            api(projects.core.navigation)
            api(projects.core.platform)
            api(projects.core.settings)
            implementation(projects.core.common)
            implementation(projects.core.network)
            implementation(projects.core.nlp)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.coil.compose)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(projects.core.account)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.webkit)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jsoup)
            implementation(libs.ktor.client.core)
        }
        jvmMain.dependencies {
            implementation(projects.core.account)
            // The desktop WebView component is built on JavaFX; desktopApp ships the platform jars.
            listOf("base", "graphics", "controls", "web", "swing").forEach { module ->
                compileOnly(javafx(module))
            }
        }
        jvmTest.dependencies {
            listOf("base", "graphics", "web").forEach { module ->
                implementation(javafx(module))
            }
        }
    }
}
