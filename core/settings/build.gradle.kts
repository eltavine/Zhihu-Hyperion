plugins {
    id("zhihu.kmp.library")
}

kotlin {
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.core.ktx)
    }
}
