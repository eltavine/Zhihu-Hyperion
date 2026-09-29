/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
}

kotlin {
    android {
        namespace = "com.hrm.markdown.renderer"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("consumer-rules.pro"))
        }
    }
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "MarkdownRenderer"
            isStatic = true
        }
    }
    macosArm64()
    // Kotlin/Native only resolves the foundation internals used by the selection code through friend modules;
    // INVISIBLE_REFERENCE suppression alone leaves unresolved IR that cannot be serialized into a klib.
    targets.withType<KotlinNativeTarget>().configureEach {
        val klibConfiguration = "${name}CompileKlibraries"
        val foundationModule = "foundation-${name.lowercase()}"
        val foundationFriendModule = providers.provider {
            configurations
                .getByName(klibConfiguration)
                .incoming
                .artifactView {
                    componentFilter { identifier ->
                        identifier is ModuleComponentIdentifier &&
                            identifier.group == "org.jetbrains.compose.foundation" &&
                            identifier.module == foundationModule
                    }
                }.files
                .singleFile
        }
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add(
                    foundationFriendModule.map { "-friend-modules=${it.absolutePath}" },
                )
            }
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(project(":markdown-parser"))
            api(project(":markdown-runtime"))

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)

            implementation(project(":latex-base"))
            implementation(project(":latex-parser"))
            implementation(project(":latex-renderer"))
            implementation(project(":codehighlight-parser"))
            implementation(project(":codehighlight-render"))

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        val persistentSelectionMain = create("persistentSelectionMain") {
            dependsOn(commonMain.get())
            kotlin.srcDir("src/androidAndJvmMain/kotlin")
        }
        // Compose foundation's internal selection API differs between AndroidX (androidMain) and the
        // JetBrains fork used on desktop, macOS and iOS (skikoMain); see selection/SelectableCompat.kt.
        val skikoMain = create("skikoMain") {
            dependsOn(commonMain.get())
        }
        nativeMain {
            dependsOn(skikoMain)
        }
        androidMain {
            dependsOn(persistentSelectionMain)
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        macosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        macosMain {
            dependsOn(persistentSelectionMain)
        }
        jvmMain {
            dependsOn(persistentSelectionMain)
            dependsOn(skikoMain)
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
