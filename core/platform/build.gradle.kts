plugins {
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.settings)
            implementation(projects.core.common)
            implementation(projects.core.model)
            implementation(projects.core.account)
            api(libs.kotlinx.io.core)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.koin.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.browser)
            implementation(libs.telephoto.zoomable.image.coil3)
        }
        jvmMain.dependencies {
            implementation(libs.compose.ui.backhandler)
        }
        macosMain.dependencies {
            implementation(libs.jetbrains.navigationevent.compose)
        }
    }
}
