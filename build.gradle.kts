// Plugin versions live in gradle/libs.versions.toml; modules apply them through build-logic convention plugins.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.module.graph.assertion) apply false
}

// Single test entry point for CI: new :core or :feature modules are covered without editing workflows.
tasks.register("jvmUnitTests") {
    group = "verification"
    description = "Runs the JVM tests of :shared and every :core/:feature module, plus the app's unit tests."
    dependsOn(
        subprojects
            .filter { it.path == ":shared" || it.path.startsWith(":core:") || it.path.startsWith(":feature:") }
            .map { "${it.path}:jvmTest" },
    )
    dependsOn(":app:testLiteDebugUnitTest")
}
