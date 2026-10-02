/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Co-author: eltavine <me@eltavine.com>
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
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG

plugins {
    id("zhihu.macos.app")
    id("zhihu.module.graph")
}

kotlin {
    macosArm64 {
        binaries.executable(listOf(DEBUG)) {
            baseName = "ZhihuHyperionUiDebug"
            entryPoint = "com.github.zly2006.zhihu.macos.debug.main"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.test)
            implementation(libs.jetbrains.navigationevent.compose)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

registerMacosAppBundle(
    "Debug",
    executableBaseName = "ZhihuHyperionUiDebug",
    bundleName = "ZhihuHyperionUiDebug",
    taskSuffix = "MacosUiDebug",
)

tasks.register<Exec>("verifyMacosReleaseIsolation") {
    dependsOn(":macosApp:linkReleaseExecutableMacosArm64")
    val releaseBinary =
        project(":macosApp")
            .layout.buildDirectory
            .file("bin/macosArm64/releaseExecutable/ZhihuHyperion.kexe")
            .get()
            .asFile
            .absolutePath
    inputs.file(releaseBinary)
    commandLine(
        "/bin/bash",
        "-euo",
        "pipefail",
        "-c",
        """
        test -f "${'$'}1"
        if /usr/bin/strings "${'$'}1" | /usr/bin/grep -Eq 'ZHPP_BACKGROUND_UI_DEBUG_V1|--smoke-test|MacosAppSmokeTest|native-unhandled-exception-(smoke-)?test'; then
          echo "macOS release binary contains debug or smoke-test controls" >&2
          exit 1
        fi
        """.trimIndent(),
        "verifyMacosReleaseIsolation",
        releaseBinary,
    )
}
