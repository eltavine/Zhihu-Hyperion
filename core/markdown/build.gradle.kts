plugins {
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
    id("zhihu.kmp.android.variants")
}

configurations.configureEach {
    resolutionStrategy {
        // org.tiqian is served from the Central snapshot repository (see libs.versions.toml); keep the cache short
        // so a newly published snapshot becomes visible within minutes.
        cacheChangingModulesFor(10, "minutes")
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(projects.core.ui)
            api(projects.markdownParser)
            implementation(projects.core.account)
            implementation(projects.core.network)
            implementation(projects.latexRenderer)
            implementation(projects.markdownRenderer)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(projects.core.icons)
            implementation(libs.compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ksoup)
            implementation(libs.ktor.client.core)
        }
        // The Tiqian renderer only publishes Android and JVM artifacts; Android Lite leaves it out to keep the APK small.
        val tiqianMarkdownMain =
            create("tiqianMarkdownMain") {
                dependsOn(commonMain.get())
                dependencies {
                    implementation(libs.tiqian.markdown.compose)
                    implementation(libs.tiqian.math.font.stix)
                }
            }
        named("androidFull") {
            dependsOn(tiqianMarkdownMain)
        }
        jvmMain {
            dependsOn(tiqianMarkdownMain)
        }
        jvmTest.dependencies {
            implementation(libs.jsoup)
        }
    }
}
