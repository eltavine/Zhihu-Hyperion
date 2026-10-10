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
                    ":core:(common|settings|nlp|icons|glass) -X> :.*",
                    ":core:model -X> :(?!core:common$).*",
                    ":core:(navigation|network) -X> :(?!core:(common|model)$).*",
                    ":core:account -X> :(?!core:(common|model|network)$).*",
                    ":core:database -X> :(?!core:common$).*",
                    ":core:designsystem -X> :(?!core:settings$).*",
                    ":core:platform -X> :(?!core:(common|model|network|account|settings|icons)$).*",
                    ":core:notification -X> :(?!core:(common|model|network|settings|platform)$).*",
                    ":core:data -X> :(?!core:(common|model|navigation|network|account|database|settings|nlp)$).*",
                    ":core:ui -X> :(?!core:(common|model|navigation|network|account|database|settings|designsystem|icons|platform|nlp|data|glass)$).*",
                    ":core:markdown -X> :(?!(core:(common|model|navigation|network|account|database|settings|designsystem|icons|platform|nlp|data|ui)|(markdown|latex|codehighlight)-.*)$).*",
                )
            }
        }
    }

    private companion object {
        const val VENDORED = ":(markdown|latex|codehighlight)-.*"
        const val APPLICATIONS = "app|desktopApp|macosApp|macosUiDebug|iosApp|sentence_embeddings"
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
