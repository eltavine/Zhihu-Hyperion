# Android Full/Lite 的 KMP 构建

`app` 保留 `full` 和 `lite` product flavor。KMP 模块使用的 Android KMP 插件只提供一份 Android 主编译，因此 `zhihu.kmp.android.variants` 约定插件会从本次调用的任务名推断要编译哪一版的 `actual`：

```bash
./gradlew :app:assembleLiteDebug
./gradlew :app:assembleFullDebug
```

产物分别位于 `app/build/outputs/apk/lite/debug/` 和 `app/build/outputs/apk/full/debug/`。Release 包使用 `assembleLiteRelease` 或 `assembleFullRelease`。

应用该插件的模块（`core:platform`、`core:ui`、`core:markdown`、`shared`）在 `src/androidMain` 放两版共用的 Android 代码，在 `src/androidFull` 和 `src/androidLite` 分别放版本专属的 `actual`。提椠渲染器只接入 `androidFull`，Lite APK 因此不打包提椠依赖。插件识别 `assemble`、`bundle`、`install`、`test`、`connected`、`compile` 和 `detekt` 开头、紧跟 `Full` 或 `Lite` 的任务。

构建两个 flavor 时必须执行两次 Gradle 命令。任务名里没有版本的调用（例如 `jvmUnitTests`、`checkKotlinAbi`、`ktlintCheck` 或 IDE 同步）使用 Lite，与 app 的默认 flavor 一致，所以提交的 Android ABI dump 记录的是 Lite 版本，`updateKotlinAbi` 也应在不带 Full 任务的调用中运行。如果这类调用还要编译另一个 flavor 的 app 代码（例如 `assembleRelease`，或会同时调度两套 Android 单元测试的全局 `test`），构建会在任务图就绪时报错，应改用对应 flavor 的任务，例如 `:app:testFullDebugUnitTest` 和 `:app:testLiteDebugUnitTest` 分两次运行。
