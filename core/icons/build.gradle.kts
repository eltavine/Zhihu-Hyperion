plugins {
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
}

kotlin {
    android {
        // The Android KMP library plugin packages Compose resources into the APK only when Android resources are
        // enabled; without it every icon throws MissingResourceException at runtime.
        androidResources.enable = true
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.components.resources)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
    }
}
