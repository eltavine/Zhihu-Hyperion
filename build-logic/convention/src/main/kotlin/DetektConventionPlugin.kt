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

import com.github.zly2006.zhihu.buildlogic.libs
import com.github.zly2006.zhihu.buildlogic.version
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * detekt with only the rules listed in `config/detekt/detekt.yml`.
 *
 * Rules that need type resolution run in the `detektMain<Target>` tasks, which detekt generates only for JVM and
 * Android compilations; common code is analyzed through them, Apple-only source sets are not.
 */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.detekt")
            extensions.configure<DetektExtension> {
                toolVersion.set(libs.version("detekt"))
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                buildUponDefaultConfig.set(false)
                parallel.set(true)
                basePath.set(rootDir)
            }
        }
    }
}
