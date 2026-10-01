import com.github.zly2006.zhihu.buildlogic.library
import com.github.zly2006.zhihu.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Compose Multiplatform on top of a Kotlin Multiplatform module; apply after `zhihu.kmp.library`. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("compose-runtime"))
                }
            }
            // The default package of the generated `Res` class is derived from the root project name, so renaming
            // the project would move every generated resource accessor; derive it from the module path instead.
            (extensions.getByType<ComposeExtension>() as ExtensionAware)
                .extensions
                .configure<ResourcesExtension> {
                    packageOfResClass = "zhihu${path.replace(':', '.')}.generated.resources"
                }
        }
    }
}
