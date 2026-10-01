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

import androidx.room.gradle.RoomExtension
import com.github.zly2006.zhihu.buildlogic.library
import com.github.zly2006.zhihu.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Room with exported schemas: every schema change shows up as a reviewable JSON diff under `schemas/`,
 * which is the baseline future migrations and auto-migrations are checked against.
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("androidx.room")
            }
            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    api(libs.library("androidx-room-runtime"))
                }
            }
            dependencies {
                listOf("kspAndroid", "kspJvm", "kspMacosArm64").forEach { configuration ->
                    add(configuration, libs.library("androidx-room-compiler"))
                }
            }
        }
    }
}
