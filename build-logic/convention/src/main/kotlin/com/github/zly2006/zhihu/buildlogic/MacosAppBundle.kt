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
        "APP_VERSION_CODE" to providers.gradleProperty("app.versionCode").get(),
    )

    val syncApp = tasks.register<Sync>("sync$buildType$taskSuffix") {
        dependsOn(
            "link${buildType}ExecutableMacosArm64",
            ":app:prepareLibraryDefinitionsLiteDebug",
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
        from(rootProject.file("desktopApp/src/main/resources/desktop-icon.png")) {
            into("Contents/Resources")
        }
        from(rootProject.file("app/build/generated/aboutLibraries/liteDebug/res/raw/aboutlibraries.json")) {
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
