import com.github.zly2006.zhihu.buildlogic.alignComposeMaterial3
import com.github.zly2006.zhihu.buildlogic.library
import com.github.zly2006.zhihu.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Kotlin/Native macOS executable that hosts the shared Compose UI; bundles are registered with `registerMacosAppBundle`. */
class MacosAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("zhihu.ktlint")
            }
            extensions.configure<KotlinMultiplatformExtension> {
                macosArm64 {
                    binaries.all {
                        // Local Xcode releases regularly run ahead of the version Kotlin/Native was validated against.
                        freeCompilerArgs += "-Xoverride-konan-properties=ignoreXcodeVersionCheck=true"
                    }
                }
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("compose-runtime"))
                }
            }
            alignComposeMaterial3()
        }
    }
}
