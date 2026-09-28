import com.jraska.module.graph.assertion.GraphRulesExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Enforces the module architecture on everything reachable from an application entry point.
 * `assertModuleGraph` runs as part of `check`.
 *
 * Layers, from the bottom: vendored libraries (`:markdown-*`, `:latex-*`, `:codehighlight-*`), `:core:*`,
 * `:feature:*`, the shared app shell `:shared`, and the platform applications. Dependencies only point down.
 */
class ModuleGraphConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.jraska.module.graph.assertion")
            extensions.configure<GraphRulesExtension> {
                configurations = setOf("api", "implementation", "fullImplementation", "liteImplementation") +
                    KMP_SOURCE_SETS.flatMap { sourceSet -> listOf("${sourceSet}Api", "${sourceSet}Implementation") }
                restricted = arrayOf(
                    "$VENDORED -X> :(core:.*|feature:.*|shared|$APPLICATIONS)",
                    ":core:.* -X> :(feature:.*|shared|$APPLICATIONS)",
                    ":feature:.* -X> :(shared|$APPLICATIONS)",
                    // Features are independent vertical slices; shared code between them belongs in :core.
                    ":feature:.* -X> :feature:.*",
                    ":shared -X> :($APPLICATIONS)",
                    // Foundation layers of :core, bottom first.
                    ":core:common -X> :.*",
                    ":core:model -X> :(?!core:common$).*",
                    ":core:(navigation|network) -X> :(?!core:(common|model)$).*",
                    ":core:account -X> :(?!core:(common|model|network)$).*",
                    ":core:database -X> :(?!core:common$).*",
                )
            }
        }
    }

    private companion object {
        const val VENDORED = ":(markdown|latex|codehighlight)-.*"
        const val APPLICATIONS = "app|desktopApp|macosApp|macosUiDebug|sentence_embeddings"
        val KMP_SOURCE_SETS = listOf(
            "commonMain",
            "androidMain",
            "jvmMain",
            "nativeMain",
            "appleMain",
            "macosMain",
            "iosMain",
        )
    }
}
