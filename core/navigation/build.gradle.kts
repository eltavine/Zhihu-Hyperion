plugins {
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core.model)
        api(libs.jetbrains.navigation.compose)
        implementation(projects.core.common)
        implementation(libs.ktor.http)
    }
}
