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

package com.github.zly2006.zhihu.buildlogic

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.filter
import org.gradle.kotlin.dsl.register

/**
 * Assembles `build/bin/macosArm64/<buildType>App/<bundleName>.app` from the linked executable, registers
 * `sync<BuildType><taskSuffix>`, `validateAndSign<BuildType><taskSuffix>` and `package<BuildType><taskSuffix>`,
 * and returns the bundle directory.
 */
fun Project.registerMacosAppBundle(
    buildType: String,
    executableBaseName: String,
    bundleName: String,
    taskSuffix: String,
): Provider<Directory> {
    val buildTypeDirectory = buildType.lowercase()
    val appDirectory = layout.buildDirectory.dir("bin/macosArm64/${buildTypeDirectory}App/$bundleName.app")
    val sharedProject = project(":shared")
    val composeResources = sharedProject.layout.buildDirectory.dir("kotlin-multiplatform-resources/aggregated-resources/macosArm64")
    val versionTokens = mapOf(
        "APP_VERSION_NAME" to providers.gradleProperty("app.versionName").get(),
        "APP_VERSION_CODE" to appVersionCode().toString(),
        "APP_GIT_COMMIT" to gitShortHash(),
    )

    val syncApp = tasks.register<Sync>("sync$buildType$taskSuffix") {
        dependsOn(
            "link${buildType}ExecutableMacosArm64",
            "exportLibraryDefinitions",
            ":shared:macosArm64AggregateResources",
        )
        into(appDirectory)
        from("src/macosMain/resources/Info.plist") {
            into("Contents")
            filter(ReplaceTokens::class, "tokens" to versionTokens)
        }
        from(layout.buildDirectory.file("bin/macosArm64/${buildTypeDirectory}Executable/$executableBaseName.kexe")) {
            into("Contents/MacOS")
            rename("$executableBaseName\\.kexe", executableBaseName)
        }
        from(rootProject.file("misc/emoji_mapping.json")) {
            into("Contents/Resources/misc")
        }
        from(rootProject.file("misc/emojis")) {
            into("Contents/Resources/misc/emojis")
        }
        from("src/macosMain/resources/AppIcon.icns") {
            into("Contents/Resources")
        }
        from(layout.buildDirectory.file(ABOUT_LIBRARIES_EXPORT)) {
            into("Contents/Resources")
        }
        from(composeResources) {
            into("Contents/Resources/compose-resources")
        }
    }
    val validateAndSign = tasks.register<ValidateAndSignMacosApp>("validateAndSign$buildType$taskSuffix") {
        dependsOn(syncApp)
        appBundle.set(appDirectory)
        this.composeResources.set(composeResources)
        requiredBundleFiles.set(
            listOf(
                "Contents/Info.plist",
                "Contents/MacOS/$executableBaseName",
                "Contents/Resources/aboutlibraries.json",
                "Contents/Resources/misc/emoji_mapping.json",
            ),
        )
    }
    tasks.register("package$buildType$taskSuffix") {
        dependsOn(validateAndSign)
    }
    return appDirectory
}
