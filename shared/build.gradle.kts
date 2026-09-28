/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
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
    alias(libs.plugins.kotlin.serialization)
}

configurations.configureEach {
    resolutionStrategy {
        // org.tiqian is served from the Central snapshot repository (see libs.versions.toml); keep the cache short
        // so a newly published snapshot becomes visible within minutes.
        cacheChangingModulesFor(10, "minutes")
    }
}

kotlin {
    android {
        namespace = "com.github.zly2006.zhihu.shared"
        androidResources {
            enable = true
        }
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(projects.core.model)
            api(projects.core.navigation)
            api(projects.core.network)
            api(projects.core.account)
            api(projects.core.database)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.core)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.material.kolor)
            implementation(libs.ksoup)
            implementation(projects.latexRenderer)
            implementation(projects.markdownParser)
            implementation(projects.markdownRenderer)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlincrypto.hmac.sha1)
            implementation(libs.aboutlibraries.compose.m3)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        // The Tiqian renderer only publishes Android and JVM artifacts.
        val tiqianMarkdownMain =
            create("tiqianMarkdownMain") {
                dependsOn(commonMain.get())
                dependencies {
                    implementation(libs.tiqian.markdown.compose)
                    implementation(libs.tiqian.math.font.stix)
                }
            }
        androidMain {
            dependsOn(tiqianMarkdownMain)
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.browser)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.livedata.ktx)
                implementation(libs.androidx.media)
                implementation(libs.androidx.webkit)
                implementation(libs.zxing.android.embedded)
                implementation(libs.zxing.core)
                implementation(libs.coil.gif)
                implementation(libs.coil.network.ktor3)
                implementation(libs.ktor.client.android)
                implementation(libs.telephoto.zoomable.image.coil3)
                implementation(libs.jsoup)
            }
        }
        jvmMain {
            dependsOn(tiqianMarkdownMain)
            dependencies {
                implementation(libs.compose.ui.backhandler)
                implementation(libs.androidx.navigationevent)
                implementation(libs.coil.network.ktor3)
                implementation(libs.androidx.sqlite.bundled)
                implementation(compose.desktop.currentOs)
                implementation(libs.zxing.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.swing)
                // JavaFX WebView hosts the desktop risk-control verification page. compileOnly keeps the
                // platform-specific jars out of consumers; desktopApp ships them itself.
                listOf("base", "graphics", "controls", "web", "swing").forEach { module ->
                    compileOnly(javafx(module))
                }
            }
        }
        macosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.jetbrains.navigationevent.compose)
        }
        jvmTest.dependencies {
            implementation(libs.jsoup)
        }
    }
}
