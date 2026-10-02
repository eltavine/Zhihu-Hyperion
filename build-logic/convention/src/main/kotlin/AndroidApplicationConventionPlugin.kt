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

import com.android.build.api.dsl.ApplicationExtension
import com.github.zly2006.zhihu.buildlogic.alignComposeMaterial3
import com.github.zly2006.zhihu.buildlogic.libs
import com.github.zly2006.zhihu.buildlogic.selectedAndroidVariant
import com.github.zly2006.zhihu.buildlogic.version
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("zhihu.ktlint")
                apply("zhihu.detekt")
            }
            extensions.configure<ApplicationExtension> {
                compileSdk = libs.version("android-compileSdk").toInt()
                defaultConfig {
                    minSdk = libs.version("android-minSdk").toInt()
                    targetSdk = libs.version("android-targetSdk").toInt()
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
            alignComposeMaterial3()

            // KMP libraries compile only the selected flavor's actuals; without this check an aggregate task such as
            // assembleRelease would package the other app flavor with the wrong ones.
            val selectedVariant = selectedAndroidVariant
            val otherVariantCompile = "$path:compile" + if (selectedVariant == "Full") "Lite" else "Full"
            gradle.taskGraph.whenReady {
                val mismatched = allTasks.firstOrNull { it.path.startsWith(otherVariantCompile) } ?: return@whenReady
                throw GradleException(
                    "${mismatched.path} cannot run while the Android $selectedVariant actuals are selected; " +
                        "name the flavor in the task (e.g. assembleFullDebug) and build Full and Lite in separate invocations",
                )
            }
        }
    }
}
