/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
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

import com.github.zly2006.zhihu.buildlogic.registerMacosAppBundle
import org.gradle.api.tasks.Exec

plugins {
    id("zhihu.macos.app")
    id("zhihu.module.graph")
}

kotlin {
    macosArm64 {
        binaries.executable {
            baseName = "ZhihuHyperion"
            entryPoint = "com.github.zly2006.zhihu.macos.main"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

registerMacosAppBundle("Release", executableBaseName = "ZhihuHyperion", bundleName = "Zhihu-Hyperion", taskSuffix = "MacosApp")
val debugApp = registerMacosAppBundle("Debug", executableBaseName = "ZhihuHyperion", bundleName = "Zhihu-Hyperion", taskSuffix = "MacosApp")

tasks.named<Exec>("runDebugExecutableMacosArm64") {
    dependsOn("syncDebugMacosApp")
    executable =
        debugApp
            .get()
            .file("Contents/MacOS/ZhihuHyperion")
            .asFile.absolutePath
}
