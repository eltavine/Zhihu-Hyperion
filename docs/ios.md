# iOS 应用

iOS 版与 Android、桌面、macOS 共用同一套 Compose 界面和全部业务代码。iOS 侧只提供窗口宿主与平台能力实现，不另写原生页面，也不复制业务模型。

## 结构

| 部分 | 位置 | 说明 |
| --- | --- | --- |
| 宿主 | `iosApp/ZhihuHyperion/ZhihuHyperionApp.swift` | SwiftUI `@main App` + `WindowGroup`，用 `UIViewControllerRepresentable` 承载 `mainViewController()` 返回的 `ComposeUIViewController` |
| Kotlin 入口 | `iosApp/src/iosMain/.../MainViewController.kt` | `startZhihuApp()` 在 `App` 初始化时安装未捕获异常钩子并启动 Koin；`mainViewController()` 挂载 `NativeZhihuMain()` |
| 框架 | `:iosApp`（`zhihu.ios.app` 约定插件） | 产出 iosArm64 与 iosSimulatorArm64 的静态框架 `ZhihuHyperionKit` |
| Xcode 工程 | `iosApp/project.yml` → `ZhihuHyperion.xcodeproj` | 由 XcodeGen 生成并提交；CI 重新生成后不得出现差异 |
| 包内资源与版本 | `syncIosAppBundle`（`build-logic/.../IosAppBundle.kt`） | 构建末尾复制表情映射、表情图片与开源许可数据，并把版本号、versionCode 和 commit 写进 Info.plist |
| Apple 共用实现 | 各模块的 `nativeMain` | macOS 与 iOS 共用：properties 设置文件、Room 数据库（打包的 SQLite 驱动）、Core Image 二维码、`AVSpeechSynthesizer` 朗读、应用内 Snackbar 消息、`NativeZhihuMain` 导航外壳 |

选型理由：

- **UIScene 生命周期**：Apple 在 [TN3187](https://developer.apple.com/documentation/technotes/tn3187-migrating-to-the-uikit-scene-based-life-cycle) 中说明，iOS 26 之后的 SDK 构建的应用必须采用基于场景的生命周期，否则无法启动。SwiftUI 的 `App`/`WindowGroup` 天然满足，不需要再写 `AppDelegate`/`SceneDelegate`。
- **直接集成框架**：Kotlin 官方推荐的 [direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html) 由 Xcode 构建阶段调用 `embedAndSignAppleFrameworkForXcode`，不引入 CocoaPods 或 SwiftPM 依赖管理。
- **XcodeGen**：`project.pbxproj` 由工具生成，评审只看 `project.yml`，也避免多人改工程文件时的合并冲突。
- **数据目录**：账号、设置、数据库都在 Application Support。它不出现在“文件”App 里，但会随设备备份。

## 构建与打包

| 目的 | 命令 |
| --- | --- |
| 在模拟器上运行 | 用 Xcode 打开 `iosApp/ZhihuHyperion.xcodeproj` 运行 |
| 模拟器包 | `iosApp/scripts/package-ios.sh simulator <out.zip>` |
| 未签名 IPA | `iosApp/scripts/package-ios.sh device <out.ipa>` |
| 改了 `project.yml` | `xcodegen generate --spec iosApp/project.yml`，并提交生成的工程 |

- 模拟器包按 “Sign to Run Locally” 临时签名；IPA 关闭代码签名构建，要用 AltStore、SideStore、Sideloadly 或自己的证书签名后才能安装。
- 装到自己的设备上：在 `iosApp/Configuration/Local.xcconfig`（不进 git）写 `DEVELOPMENT_TEAM = <团队 ID>`。
- PR 的 CI 任务 `iOS simulator debug build` 只构建 Debug 模拟器应用，不产出包：在 GitHub 的 macOS 机器上，每个 Release 目标要构建 27–39 分钟。发布流程用 Release 构建 `zhihu-hyperion-ios-arm64-unsigned.ipa`，附在 nightly 与正式版上，`update.json` 的 `ios-arm64` 指向它。iOS 不能在应用内安装更新，检查到新版本时打开下载页。
- Kotlin/Native 在 Gradle 守护进程里链接 Release 二进制，整程序优化需要超过 4 GiB 堆，`gradle.properties` 因此设为 5 GiB。

## 交互与系统适配

| 项目 | 现状 |
| --- | --- |
| 安全区与键盘 | 页面自己消费 `WindowInsets`；宿主 `ignoresSafeArea()`。iOS 把状态栏区域的触摸交给系统（点击回到顶部），控件必须避开 `safeDrawing`，否则点不到 |
| 侧滑返回 | Compose Multiplatform 1.12 自带左缘滑动识别，经 navigation event dispatcher 触发 `BackHandler` 与导航返回；已用 XCUITest 从屏幕左缘拖动验证 |
| 深浅色 | 跟随系统时清除 `overrideUserInterfaceStyle`，系统切换能传进来；指定亮/暗色时固定窗口外观，状态栏与键盘随应用主题 |
| 刷新率 | Info.plist 设置 `CADisableMinimumFrameDurationOnPhone`，ProMotion 屏幕以 120 Hz 渲染 |
| 朗读 | `AVSpeechSynthesizer`，音频会话为 `playback` + `spokenAudio`，`UIBackgroundModes` 含 `audio`，锁屏后继续 |
| 登录 | 手机号与扫码；网页登录依赖系统 WebView，iOS 不提供 |
| 底部导航 | 最多 5 项（`platformBottomBarItemLimit`） |

## 能力缺口

以下能力在 iOS 上尚未达到 Android 的产品质量，按“平台 actual 的质量门槛”保持禁用或降级，不用低质量替代品冒充：

| 能力 | 当前行为 | 目标实现 |
| --- | --- | --- |
| 图片查看 | 在浏览器中打开 | 应用内查看器（与 Android 共用 Compose 实现） |
| 保存图片 | 写入应用私有目录，用户看不到 | 存入“照片”（`PHPhotoLibrary`，仅添加权限） |
| 分享 | 复制链接 | 系统分享面板（`UIActivityViewController`） |
| 视频 | 不支持应用内播放 | `AVPlayerViewController` |
| 编辑器选图 | 不支持 | `PHPickerViewController` |
| 屏蔽列表导入 | 不支持选择文件 | `UIDocumentPickerViewController` |
| 文章导出 HTML/长图 | 不支持 | 待定 |
| 通知 | 不支持 | iOS 不允许后台轮询，需要另行设计 |
| 语义屏蔽 | 不支持 | 端侧模型只在 Android Full 版提供 |
