plugins {
    id("zhihu.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(projects.core.account)
            implementation(projects.core.settings)
            implementation(projects.core.platform)
            implementation(libs.androidx.core.ktx)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
