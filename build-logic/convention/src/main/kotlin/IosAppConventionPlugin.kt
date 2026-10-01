import com.github.zly2006.zhihu.buildlogic.IOS_FRAMEWORK_NAME
import com.github.zly2006.zhihu.buildlogic.alignComposeMaterial3
import com.github.zly2006.zhihu.buildlogic.library
import com.github.zly2006.zhihu.buildlogic.libs
import com.github.zly2006.zhihu.buildlogic.registerIosAppBundleSync
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Kotlin/Native iOS framework that exports the shared Compose UI to the SwiftUI host in `iosApp/`.
 *
 * Xcode builds the app: its build phases call `embedAndSignAppleFrameworkForXcode` before compiling Swift and
 * `syncIosAppBundle` after copying resources (see `iosApp/project.yml`).
 */
class IosAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("zhihu.ktlint")
                apply("zhihu.aboutlibraries")
            }
            extensions.configure<KotlinMultiplatformExtension> {
                listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
                    iosTarget.binaries.framework {
                        baseName = IOS_FRAMEWORK_NAME
                        isStatic = true
                        // Local Xcode releases regularly run ahead of the version Kotlin/Native was validated against.
                        freeCompilerArgs += "-Xoverride-konan-properties=ignoreXcodeVersionCheck=true"
                    }
                }
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("compose-runtime"))
                }
            }
            alignComposeMaterial3()
            registerIosAppBundleSync()
        }
    }
}
