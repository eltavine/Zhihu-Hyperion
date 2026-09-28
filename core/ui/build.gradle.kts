import com.github.zly2006.zhihu.buildlogic.javafx

plugins {
    id("zhihu.kmp.library")
    id("zhihu.kmp.compose")
}

kotlin {
    android {
        // The Android KMP library plugin only packages Compose resources (the author badge icon) into the APK's assets
        // when Android resources are enabled; without it AuthorBadge throws MissingResourceException at runtime.
        androidResources.enable = true
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.data)
            api(projects.core.database)
            api(projects.core.designsystem)
            api(projects.core.model)
            api(projects.core.navigation)
            api(projects.core.platform)
            api(projects.core.settings)
            implementation(projects.core.common)
            implementation(projects.core.network)
            implementation(projects.core.nlp)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.coil.compose)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(projects.core.account)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.webkit)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jsoup)
            implementation(libs.ktor.client.core)
        }
        jvmMain.dependencies {
            implementation(projects.core.account)
            // The desktop WebView component is built on JavaFX; desktopApp ships the platform jars.
            listOf("base", "graphics", "controls", "web", "swing").forEach { module ->
                compileOnly(javafx(module))
            }
        }
    }
}
