/*
 * Zhihu-Hyperion - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Co-author: eltavine <me@eltavine.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.github.zly2006.zhihu.buildlogic.alignComposeMaterial3
import com.github.zly2006.zhihu.buildlogic.appVersionCode
import com.github.zly2006.zhihu.buildlogic.gitShortHash
import com.github.zly2006.zhihu.buildlogic.javafx
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipFile

val appVersionName = property("app.versionName").toString()
val desktopPackageVersion = if (appVersionName.count { it == '.' } >= 2) appVersionName else "$appVersionName.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("zhihu.ktlint")
    id("zhihu.detekt")
    id("zhihu.module.graph")
    id("zhihu.aboutlibraries")
}

kotlin {
    jvmToolchain(17)
}

// The desktop build reads its own identity from this resource; jpackage launchers and the uber jar cannot pass
// version metadata any other way that `run`, packaged apps and `java -jar` all see.
val writeBuildInfo =
    tasks.register<WriteProperties>("writeBuildInfo") {
        destinationFile = layout.buildDirectory.file("generated/buildInfo/zhihu-build.properties")
        property("versionName", appVersionName)
        property("versionCode", appVersionCode())
        property("commit", gitShortHash())
    }
sourceSets.main {
    resources.srcDir(
        writeBuildInfo.map {
            it.destinationFile
                .get()
                .asFile.parentFile
        },
    )
}

alignComposeMaterial3()

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    // JavaFX WebView hosts the desktop risk-control verification page.
    listOf("base", "controls", "graphics", "web", "swing", "media").forEach { module ->
        implementation(javafx(module))
    }
}

fun currentDesktopNativeExcludes(): List<String> {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    val isArm64 = osArch == "aarch64" || osArch == "arm64"
    val sqliteTarget =
        when {
            osName.contains("mac") && isArm64 -> "osx_arm64"
            osName.contains("mac") -> "osx_x64"
            osName.contains("win") -> "windows_x64"
            osName.contains("linux") && isArm64 -> "linux_arm64"
            else -> "linux_x64"
        }
    val skikoTarget =
        when {
            osName.contains("mac") && isArm64 -> "macos-arm64"
            osName.contains("mac") -> "macos-x64"
            osName.contains("win") -> "windows-x64"
            osName.contains("linux") && isArm64 -> "linux-arm64"
            else -> "linux-x64"
        }
    val sqliteNatives =
        listOf(
            "natives/linux_arm64/**",
            "natives/linux_x64/**",
            "natives/osx_arm64/**",
            "natives/osx_x64/**",
            "natives/windows_x64/**",
        )
    val skikoNatives =
        listOf(
            "libskiko-linux-arm64.so",
            "libskiko-linux-x64.so",
            "libskiko-macos-arm64.dylib",
            "libskiko-macos-x64.dylib",
            "skiko-windows-x64.dll",
        )

    return sqliteNatives.filterNot { it == "natives/$sqliteTarget/**" } +
        skikoNatives.filterNot { it.contains(skikoTarget) }
}

tasks.withType<Jar>().configureEach {
    if (name == "packageReleaseUberJarForCurrentOS") {
        exclude(currentDesktopNativeExcludes())
    }
}

// ProGuard 看不到只经 ServiceLoader（META-INF/services）发现的实现类，会当作无用代码删掉，缺失只在 release 包运行时才暴露
// （Coil 的网络图片 fetcher 就这样失效过）。打包前核对 release jar 里每条服务登记的实现类都还在。
val verifyReleaseServiceProviders =
    tasks.register("verifyReleaseServiceProviders") {
        val proguardOutput = layout.buildDirectory.dir("compose/tmp/main-release/proguard")
        dependsOn("proguardReleaseJars")
        inputs.dir(proguardOutput)
        doLast {
            val jars =
                proguardOutput
                    .get()
                    .asFile
                    .listFiles { file -> file.extension == "jar" }
                    .orEmpty()
            check(jars.isNotEmpty()) { "没有找到 ProGuard 输出的 jar：${proguardOutput.get().asFile}" }
            val classes = mutableSetOf<String>()
            val providers = mutableMapOf<String, String>()
            jars.forEach { jar ->
                ZipFile(jar).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if (entry.name.endsWith(".class")) {
                            classes += entry.name.removeSuffix(".class").replace('/', '.')
                        } else if (entry.name.startsWith("META-INF/services/") && !entry.isDirectory) {
                            zip.getInputStream(entry).bufferedReader().useLines { lines ->
                                lines
                                    .map { it.substringBefore('#').trim() }
                                    .filter { it.isNotEmpty() }
                                    .forEach { providers[it] = entry.name.removePrefix("META-INF/services/") }
                            }
                        }
                    }
                }
            }
            val missing = providers.filterKeys { it !in classes }
            check(missing.isEmpty()) {
                "ProGuard 删掉了这些 ServiceLoader 实现类，需要在 proguard-release.pro 里保留：" +
                    missing.entries.joinToString { (impl, service) -> "$impl（$service）" }
            }
        }
    }
tasks
    .matching { it.name == "packageReleaseDistributionForCurrentOS" || it.name == "packageReleaseUberJarForCurrentOS" }
    .configureEach { dependsOn(verifyReleaseServiceProviders) }

compose.desktop {
    application {
        mainClass = "com.github.zly2006.zhihu.desktop.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(true)
            optimize.set(false)
            configurationFiles.from(project.file("proguard-release.pro"))
        }

        nativeDistributions {
            // 对外发布的安装形式：Windows 用 MSI，Linux 用 AppImage（由 CI 用 appimagetool
            // 把 jpackage 的 app-image 目录封装成单文件）。macOS 走 macosApp 的 Kotlin/Native 构建。
            targetFormats(TargetFormat.AppImage, TargetFormat.Msi)
            packageName = "Zhihu-Hyperion"
            packageVersion = desktopPackageVersion
            description = "Ad-free, lightweight, AI-assisted third-party Zhihu client"
            vendor = "eltavine"

            // jlink 裁剪内嵌运行时：默认只含 java.base/java.desktop/java.logging/jdk.crypto.ec，
            // 其余模块按依赖补齐（JavaFX WebView 的 JS 互操作、JDBC、HTTP、中文扩展字符集等）。
            // 依赖变化后用 ./gradlew :desktopApp:suggestModules 校对；注意 checkRuntime 不会
            // 校验模块是否齐全，缺模块只会在启动时报错，改完列表要实际跑一次安装包。
            // JavaFX 走 classpath，suggestModules 看不到它的 module-info：javafx.swing（JFXPanel）要
            // jdk.unsupported.desktop，javafx.web 要 jdk.xml.dom，少了前者网页登录和风控验证的 WebView 根本建不起来。
            modules(
                "java.management",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "java.security.jgss",
                "java.sql",
                "java.xml",
                "jdk.charsets",
                "jdk.jsobject",
                "jdk.unsupported",
                "jdk.unsupported.desktop",
                "jdk.xml.dom",
                "jdk.zipfs",
            )

            linux {
                iconFile.set(project.file("src/main/resources/desktop-icon.png"))
            }
            windows {
                iconFile.set(project.file("desktop-icon.ico"))
                menu = true
                menuGroup = "Zhihu-Hyperion"
                shortcut = true
                upgradeUuid = "488DBBD3-16F8-4CC0-AC04-2ECA1616E609"
            }
        }
    }
}
