plugins {
    `kotlin-dsl`
}

group = "com.github.zly2006.zhihu.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.multiplatform.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.module.graph.assertion.gradlePlugin)
    compileOnly(libs.aboutlibraries.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("ktlint") {
            id = "zhihu.ktlint"
            implementationClass = "KtlintConventionPlugin"
        }
        register("kmpLibrary") {
            id = "zhihu.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "zhihu.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpAndroidVariants") {
            id = "zhihu.kmp.android.variants"
            implementationClass = "KmpAndroidVariantsConventionPlugin"
        }
        register("androidApplication") {
            id = "zhihu.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("room") {
            id = "zhihu.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("macosApp") {
            id = "zhihu.macos.app"
            implementationClass = "MacosAppConventionPlugin"
        }
        register("iosApp") {
            id = "zhihu.ios.app"
            implementationClass = "IosAppConventionPlugin"
        }
        register("moduleGraph") {
            id = "zhihu.module.graph"
            implementationClass = "ModuleGraphConventionPlugin"
        }
        register("aboutLibraries") {
            id = "zhihu.aboutlibraries"
            implementationClass = "AboutLibrariesConventionPlugin"
        }
    }
}
