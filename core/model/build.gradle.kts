plugins {
    id("zhihu.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.serialization.json)
        implementation(projects.core.common)
        implementation(libs.ksoup)
    }
}
