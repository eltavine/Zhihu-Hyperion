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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/** Name of the Kotlin framework the SwiftUI host imports; `iosApp/project.yml` links the same name. */
const val IOS_FRAMEWORK_NAME = "ZhihuHyperionKit"

/**
 * Registers `syncIosAppBundle`, run by Xcode after it copies the app's own resources.
 *
 * Native code reads `misc/` and `aboutlibraries.json` from the bundle root (see `nativeBundledResourcePath`), and
 * reads the version and commit from `Info.plist`. Gradle owns those values, so this task writes them into the
 * built product; the placeholders in the Xcode project never ship.
 */
fun Project.registerIosAppBundleSync() {
    tasks.register<SyncIosAppBundle>("syncIosAppBundle") {
        dependsOn("exportLibraryDefinitions")
        versionName.set(providers.gradleProperty("app.versionName"))
        versionCode.set(appVersionCode().toString())
        gitCommit.set(gitShortHash())
        emojiMapping.set(rootProject.layout.projectDirectory.file("misc/emoji_mapping.json"))
        emojiDirectory.set(rootProject.layout.projectDirectory.dir("misc/emojis"))
        libraryDefinitions.set(layout.buildDirectory.file(ABOUT_LIBRARIES_EXPORT))
        targetBuildDirectory.set(providers.environmentVariable("TARGET_BUILD_DIR"))
        resourcesFolderPath.set(providers.environmentVariable("UNLOCALIZED_RESOURCES_FOLDER_PATH"))
        infoPlistPath.set(providers.environmentVariable("INFOPLIST_PATH"))
    }
}

abstract class SyncIosAppBundle : DefaultTask() {
    @get:Input abstract val versionName: Property<String>

    @get:Input abstract val versionCode: Property<String>

    @get:Input abstract val gitCommit: Property<String>

    @get:InputFile abstract val emojiMapping: RegularFileProperty

    @get:InputDirectory abstract val emojiDirectory: DirectoryProperty

    @get:InputFile abstract val libraryDefinitions: RegularFileProperty

    /** Xcode build settings, available only when Xcode runs the task from a build phase. */
    @get:Internal abstract val targetBuildDirectory: Property<String>

    @get:Internal abstract val resourcesFolderPath: Property<String>

    @get:Internal abstract val infoPlistPath: Property<String>

    @get:Inject abstract val fileSystem: FileSystemOperations

    @get:Inject abstract val exec: ExecOperations

    @TaskAction
    fun sync() {
        if (!targetBuildDirectory.isPresent || !resourcesFolderPath.isPresent || !infoPlistPath.isPresent) {
            throw GradleException("syncIosAppBundle runs from the iosApp Xcode build phase; build the app with Xcode or xcodebuild.")
        }
        val buildDirectory = File(targetBuildDirectory.get())
        val resources = File(buildDirectory, resourcesFolderPath.get())
        fileSystem.copy {
            from(emojiMapping)
            into(File(resources, "misc"))
        }
        fileSystem.copy {
            from(emojiDirectory)
            into(File(resources, "misc/emojis"))
        }
        fileSystem.copy {
            from(libraryDefinitions)
            into(resources)
        }
        val infoPlist = File(buildDirectory, infoPlistPath.get())
        mapOf(
            "CFBundleShortVersionString" to versionName.get(),
            "CFBundleVersion" to versionCode.get(),
            "ZhihuGitCommit" to gitCommit.get(),
        ).forEach { (key, value) ->
            exec.exec { commandLine("plutil", "-replace", key, "-string", value, infoPlist.absolutePath) }
        }
    }
}
