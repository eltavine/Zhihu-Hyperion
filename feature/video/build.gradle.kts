plugins {
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.account)
            implementation(projects.core.common)
            implementation(projects.core.model)
            implementation(projects.core.navigation)
            implementation(projects.core.platform)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.koin.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(projects.core.settings)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui.compose)
            implementation(libs.androidx.media3.ui.compose.material3)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
