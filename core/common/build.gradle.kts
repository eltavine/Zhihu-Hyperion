plugins {
    id("zhihu.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kermit)
        implementation(libs.kotlinx.datetime)
    }
}
