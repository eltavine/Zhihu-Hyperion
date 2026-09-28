import com.github.zly2006.zhihu.buildlogic.ABOUT_LIBRARIES_EXPORT
import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * Open-source notices for the non-Android apps: each app exports the definitions of its own dependency
 * graph instead of borrowing the Android build's output. JVM apps load the export from their classpath;
 * the macOS bundle task copies it into `Contents/Resources`.
 */
class AboutLibrariesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.mikepenz.aboutlibraries.plugin")
            extensions.configure<AboutLibrariesExtension> {
                collect {
                    configPath.set(rootProject.layout.projectDirectory.dir("aboutlibraries"))
                }
                export {
                    outputFile.set(layout.buildDirectory.file(ABOUT_LIBRARIES_EXPORT))
                }
            }
            pluginManager.withPlugin("java") {
                val exportDirectory = layout.buildDirectory.file(ABOUT_LIBRARIES_EXPORT).map { it.asFile.parentFile }
                extensions.getByType<SourceSetContainer>().named("main") {
                    resources.srcDir(files(exportDirectory).builtBy("exportLibraryDefinitions"))
                }
            }
        }
    }
}
