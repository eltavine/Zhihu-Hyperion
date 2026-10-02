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
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
    id("zhihu.kmp.android.variants")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.settings)
            implementation(projects.core.common)
            implementation(projects.core.model)
            implementation(projects.core.account)
            implementation(projects.core.icons)
            api(libs.kotlinx.io.core)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.koin.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.browser)
            implementation(libs.telephoto.zoomable.image.coil3)
        }
        jvmMain.dependencies {
            implementation(libs.compose.ui.backhandler)
        }
        macosMain.dependencies {
            implementation(libs.jetbrains.navigationevent.compose)
        }
        // telephoto 的缩放手势支持 iOS 但没有 macOS 目标，所以只有 iOS 用它做应用内看图。
        iosMain.dependencies {
            implementation(libs.telephoto.zoomable)
            implementation(libs.coil.compose)
        }
    }
}
