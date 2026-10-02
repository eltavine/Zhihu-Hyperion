-dontwarn com.hrm.latex.parser.tokenizer.LatexTokenizer$Companion

# JavaFX 的工具包和原生窗口栈只能反射加载（javafx.toolkit 属性默认指向
# com.sun.javafx.tk.quantum.QuantumToolkit，glass/prism 同理），静态分析不可达；
# 缺失时运行时报 "No toolkit found"。WebView 的 WebKit 引擎由原生代码按名字回调
# com.sun.webkit 里的私有 Java 方法（WebPage.fwkFrameCreated 等），裁掉后 release 包里的
# WebView 连 jfxwebkit 都加载不了，网页登录和扫码登录的安全验证随之失效。
# JavaFX 内部反射和 JNI 回调遍布，所有包（下面是 JavaFX 21 jar 里的全部顶层包）都整包保留。
-keep class javafx.** { *; }
-keep class com.sun.javafx.** { *; }
-keep class com.sun.glass.** { *; }
-keep class com.sun.prism.** { *; }
-keep class com.sun.scenario.** { *; }
-keep class com.sun.pisces.** { *; }
-keep class com.sun.openpisces.** { *; }
-keep class com.sun.marlin.** { *; }
-keep class com.sun.webkit.** { *; }
-keep class com.sun.media.** { *; }
-keep class com.sun.openjfx.** { *; }
-keep class netscape.javascript.** { *; }

-keep class * implements io.ktor.client.HttpClientEngineContainer {
    *;
}

# Coil 只通过 ServiceLoader（META-INF/services/coil3.util.FetcherServiceLoaderTarget）找到 Ktor 的网络图片
# fetcher，代码里没有直接引用；裁掉后 release 包里的网络图片全部加载不出来。
-keep class * implements coil3.util.FetcherServiceLoaderTarget {
    *;
}

-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider {
    *;
}

-keep class io.ktor.client.engine.cio.CIOEngineContainer {
    *;
}

-keep class io.ktor.client.engine.java.JavaHttpEngineContainer {
    *;
}

-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.PaginationEnvironment {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.CollectionContentEnvironment {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.NotificationPaginationEnvironment {
    *;
}

-keep class * implements com.github.zly2006.zhihu.viewmodel.PaginationEnvironment {
    *;
}

-keep class com.github.zly2006.zhihu.viewmodel.filter.*_Impl {
    *;
}

-keep class com.github.zly2006.zhihu.viewmodel.local.*_Impl {
    *;
}

-keep class * extends androidx.room.RoomDatabase {
    *;
}

-keep @androidx.room.Database class * {
    *;
}

-keep @androidx.room.Dao class * {
    *;
}

-keep class androidx.sqlite.driver.bundled.** {
    *;
}

-keepclasseswithmembernames class * {
    native <methods>;
}
