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

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.github.zly2006.zhihu.buildlogic.alignComposeMaterial3
import com.github.zly2006.zhihu.buildlogic.defaultAndroidNamespace
import com.github.zly2006.zhihu.buildlogic.libs
import com.github.zly2006.zhihu.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

/**
 * Kotlin Multiplatform library targeting every platform Zhihu-Hyperion ships on: Android, desktop JVM and macOS,
 * plus iOS so shared code keeps compiling for the next Apple host.
 *
 * Modules only declare what differs from this baseline; override the Android namespace with
 * `kotlin { android { namespace = "..." } }` when an existing `R` package must be preserved.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
                apply("zhihu.ktlint")
                apply("zhihu.detekt")
            }
            extensions.configure<KotlinMultiplatformExtension> {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
                (this as ExtensionAware).extensions.getByName<KotlinMultiplatformAndroidLibraryTarget>("android").apply {
                    namespace = defaultAndroidNamespace()
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }
                jvm {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }
                macosArm64()
                iosArm64()
                iosSimulatorArm64()

                sourceSets.commonTest.dependencies {
                    implementation(kotlin("test"))
                }

                // :core modules are consumed by every feature; their public API is reviewed through committed
                // dumps (`updateKotlinAbi` to regenerate). Linux CI cannot compile the Apple targets, so their
                // klib entries are kept from the reference dump instead of failing the check there.
                if (path.startsWith(":core:")) {
                    @OptIn(ExperimentalAbiValidation::class)
                    abiValidation {
                        keepLocallyUnsupportedTargets.set(true)
                    }
                }
            }
            alignComposeMaterial3()
        }
    }
}
