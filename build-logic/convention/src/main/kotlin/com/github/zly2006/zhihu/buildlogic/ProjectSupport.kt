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
