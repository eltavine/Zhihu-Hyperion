plugins {
    id("zhihu.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.io.core)
        implementation(libs.kermit)
        implementation(libs.kotlinx.datetime)
    }
}
