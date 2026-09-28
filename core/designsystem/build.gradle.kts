plugins {
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.settings)
            api(libs.compose.material3)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.material.kolor)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
    }
}
