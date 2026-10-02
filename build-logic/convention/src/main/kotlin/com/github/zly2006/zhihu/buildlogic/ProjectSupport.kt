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

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** Build-directory path of each app's exported open-source definitions (`exportLibraryDefinitions`). */
const val ABOUT_LIBRARIES_EXPORT = "generated/aboutLibraries/aboutlibraries.json"

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()

private val androidVariantTaskPattern = Regex("^(?:assemble|bundle|install|test|connected|compile|detekt)(Full|Lite)")

/**
 * App flavor (`Full` or `Lite`) whose Android `actual`s this invocation compiles. The Android KMP library plugin has a
 * single Android compilation, so the flavor is read from the requested task names; invocations that name no flavor,
 * such as `jvmUnitTests`, `checkKotlinAbi` or an IDE sync, use Lite, the app's default flavor.
 */
internal val Project.selectedAndroidVariant: String
    get() {
        val requested = gradle.startParameter.taskNames.mapNotNullTo(mutableSetOf()) { taskName ->
            androidVariantTaskPattern.find(taskName.substringAfterLast(':'))?.groupValues?.get(1)
        }
        require(requested.size <= 1) { "Full and Lite Android tasks require separate Gradle invocations" }
        return requested.singleOrNull() ?: "Lite"
    }

/** `:core:model` becomes `com.github.zly2006.zhihu.core.model`. */
internal fun Project.defaultAndroidNamespace(): String =
    "com.github.zly2006.zhihu." + path.removePrefix(":").split(':').joinToString(".") { it.replace('-', '.') }

/**
 * material-kolor declares a strict material3 requirement that only reaches the KMP metadata configuration,
 * so platform compile/runtime classpaths would otherwise resolve different material3 versions and fail
 * with invisible/hidden API errors. Every configuration is aligned to the catalog version instead.
 */
fun Project.alignComposeMaterial3() {
    val material3 = libs.version("compose-material3")
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.compose.material3") {
                useVersion(material3)
                because("Keep every classpath on the material3 version required by material-kolor")
            }
        }
    }
}

/** Short commit hash for build metadata; builds from a source archive without git report `unknown`. */
fun Project.gitShortHash(): String = runCatching {
    providers
        .exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
        .trim()
}.getOrNull()?.takeIf { it.isNotEmpty() } ?: "unknown"

/**
 * `app.versionCodeOffset` plus the number of commits reachable from HEAD, shared by every platform build.
 *
 * A shallow clone would count only its own depth and publish a versionCode below builds users already have,
 * so it fails instead; CI checks out with `fetch-depth: 0`.
 */
fun Project.appVersionCode(): Int {
    fun git(vararg arguments: String): String = providers
        .exec {
            commandLine("git", *arguments)
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
        .trim()
    check(git("rev-parse", "--is-shallow-repository") != "true") {
        "versionCode counts git commits, so this build needs the full history (git fetch --unshallow)."
    }
    val commitCount = git("rev-list", "--count", "HEAD").toIntOrNull()
        ?: error("versionCode counts git commits, so this build must run from a git checkout.")
    return providers.gradleProperty("app.versionCodeOffset").get().toInt() + commitCount
}

/** JavaFX publishes platform-specific jars behind a Maven classifier that Gradle cannot select on its own. */
fun javafxPlatformClassifier(): String {
    val osName = System.getProperty("os.name").lowercase()
    val isArm64 = System.getProperty("os.arch").lowercase().let { it == "aarch64" || it == "arm64" }
    return when {
        osName.contains("mac") -> if (isArm64) "mac-aarch64" else "mac"
        osName.contains("win") -> "win"
        else -> if (isArm64) "linux-aarch64" else "linux"
    }
}

/** Coordinates of a JavaFX module for the current build host, e.g. `javafx("web")`. */
fun Project.javafx(module: String): String = "org.openjfx:javafx-$module:${libs.version("javafx")}:${javafxPlatformClassifier()}"
