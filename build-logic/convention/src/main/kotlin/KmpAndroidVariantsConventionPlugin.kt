import com.github.zly2006.zhihu.buildlogic.selectedAndroidVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Android Full/Lite `actual`s for a Kotlin Multiplatform module; apply after `zhihu.kmp.library`.
 *
 * `src/androidFull` and `src/androidLite` are both source sets so ktlint checks them, but only the one matching
 * [selectedAndroidVariant] joins the Android compilation; see `docs/kmp-android-variants.md`.
 */
class KmpAndroidVariantsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val selectedVariant = selectedAndroidVariant
            extensions.configure<KotlinMultiplatformExtension> {
                // Explicit dependsOn edges switch off the implicit default hierarchy (nativeMain, appleMain, ...).
                applyDefaultHierarchyTemplate()
                val commonMain = sourceSets.getByName("commonMain")
                listOf("Full", "Lite").forEach { variant ->
                    val variantMain = sourceSets.create("android$variant") { dependsOn(commonMain) }
                    if (variant == selectedVariant) {
                        sourceSets.getByName("androidMain").dependsOn(variantMain)
                    }
                }
            }
        }
    }
}
