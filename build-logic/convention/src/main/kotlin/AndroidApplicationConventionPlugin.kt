import com.android.build.api.dsl.ApplicationExtension
import com.github.zly2006.zhihu.buildlogic.alignComposeMaterial3
import com.github.zly2006.zhihu.buildlogic.libs
import com.github.zly2006.zhihu.buildlogic.selectedAndroidVariant
import com.github.zly2006.zhihu.buildlogic.version
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("zhihu.ktlint")
            }
            extensions.configure<ApplicationExtension> {
                compileSdk = libs.version("android-compileSdk").toInt()
                defaultConfig {
                    minSdk = libs.version("android-minSdk").toInt()
                    targetSdk = libs.version("android-targetSdk").toInt()
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
            alignComposeMaterial3()

            // KMP libraries compile only the selected flavor's actuals; without this check an aggregate task such as
            // assembleRelease would package the other app flavor with the wrong ones.
            val selectedVariant = selectedAndroidVariant
            val otherVariantCompile = "$path:compile" + if (selectedVariant == "Full") "Lite" else "Full"
            gradle.taskGraph.whenReady {
                val mismatched = allTasks.firstOrNull { it.path.startsWith(otherVariantCompile) } ?: return@whenReady
                throw GradleException(
                    "${mismatched.path} cannot run while the Android $selectedVariant actuals are selected; " +
                        "name the flavor in the task (e.g. assembleFullDebug) and build Full and Lite in separate invocations",
                )
            }
        }
    }
}
