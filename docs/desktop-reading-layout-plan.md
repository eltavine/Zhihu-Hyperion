# 桌面端列表-详情阅读布局改造方案

> 状态：待实施。本文只描述改动方案，仓库代码尚未修改。

## 1. 现象

桌面端开启「横屏双栏布局」后，点击左栏列表项，右栏详情"看起来很简陋，文字太宽"：

- 正文一行七十多个汉字，阅读时视线要横扫过整个屏幕。
- 左右两栏底色相同，中间只有一根 1dp 细线，大屏上几乎没有边界感。
- 分隔条没有 hover 反馈，光标也不变，用户不知道它可以拖动。
- 关闭应用再打开，窗口大小和位置不保留。

## 2. 根因

分栏框架本身是完整的（[AdaptiveListDetail.kt:56](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/AdaptiveListDetail.kt)）：

| 能力 | 实现 |
| --- | --- |
| 触发条件 | 窗口 ≥ 840dp 且横向 |
| 栏宽比例 | 默认 1/3，持久化到 `landscape_list_pane_ratio` |
| 最小宽度 | 列表 320dp / 详情 480dp |
| 分隔条 | 16dp 热区 + 1dp `outlineVariant` 线，可拖 |
| 空状态 | `EmptyDetailPane` |

问题不在分栏，在阅读列没有宽度上限。[ArticleScreen.kt:910](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreen.kt) 的正文容器只有：

```kotlin
Column(
    modifier = Modifier
        .padding(horizontal = 16.dp)   // 唯一与宽度相关的修饰符
        .verticalScroll(scrollState)
        .padding(innerPadding)
        .padding(top = 8.dp),
)
```

按双栏内容区宽度为 1920dp、列表栏默认占可分配宽度 1/3 计算：扣除 16dp 分隔条后，可分配宽度为 1904dp；左栏约 635dp、右栏约 1269dp，正文扣除两侧各 16dp 内边距后可用宽度约 1237dp。若以 16sp 中文字形粗略估算，一行可能超过 70 字；实际字数随字体、字号与显示缩放而变化。

中文正文的舒适行宽是 30–40 字，对应 640–760dp。项目里已有同类先例：[LoginScreen.kt:101](../shared/src/commonMain/kotlin/com/github/zly2006/zhihu/account/LoginScreen.kt) 用 `LoginContentMaxWidth = 560.dp` 配合 `widthIn(max = ...)` 限制引导/登录页宽度。本次沿用同一思路。

## 3. 目标

- 1360×860 窗口：正文容器最大宽度 720dp，扣除两侧各 16dp 内边距后文本可用宽度最大 688dp；每行字数随实际字体、字号和显示缩放变化。
- 1920×1080 窗口：阅读列最大宽度保持 720dp 并水平居中，两侧留白，不随窗口宽度继续增长。
- 可用宽度不足 720dp 时（手机、平板竖屏、窄窗口）布局与改动前完全一致。
- 两栏有明确层级；分隔条可被发现、可拖动。
- 窗口大小/位置/最大化状态跨启动保留。

## 4. 改动清单

### P0-1 正文最大宽度

**目标文件**

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ArticleScreen.kt`（回答/文章正文）
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/PinScreen.kt`（想法正文）
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/QuestionScreen.kt`（先确认问题详情是否复用 `ArticleScreen`；若正文由 `ArticleScreen` 承接则不重复改）

**常量位置**

新增 `ReadingContentMaxWidth = 720.dp`。优先放 `:core:designsystem` 的主题尺度（与 `Theme.kt` 同层）；若 `shared` 未直接依赖该模块，则放 `:core:ui`。不要放页面文件里重复声明。

**ArticleScreen 改法**

父容器当前是 `Box(Modifier.glassBackdropSource(glassBackdrop))`，无 `fillMaxSize()`，属 wrap-content；不加 `fillMaxSize()` 时子节点居中对齐不生效。

```kotlin
Box(Modifier.fillMaxSize().glassBackdropSource(glassBackdrop)) {
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .widthIn(max = ReadingContentMaxWidth)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(top = 8.dp),
    ) { ... }
}
```

标题、作者行、正文、底部日期都在这个 Column 内，一处改动即可覆盖全页，无需改 `RenderMarkdown`。

**PinScreen 改法**

`PinScreen.kt:520` 的 Column 用了 `fillMaxSize()`，会先把宽度固定成最大值，直接追加 `widthIn` 无效。改为在 Scaffold 内容里包一层居中容器：

```kotlin
Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        modifier = Modifier
            .widthIn(max = ReadingContentMaxWidth)
            .fillMaxHeight()
            .pageTurnViewportWithGuide(pageTurnTarget)
            .verticalScroll(scrollState)
            .testTag(PIN_SCREEN_SCROLL_TAG)
            .padding(16.dp),
    ) { ... }
}
```

**风险**

- 宽图、表格、代码块会被一并限制到 720dp。这是期望行为（与网页阅读列一致）；如需例外，单独给该区块加横向滚动，不要放宽整列。
- `pageTurnViewportWithGuide`、`AnswerVerticalOverscroll` 等手势/预览逻辑依赖容器尺寸，改完必须实机验证翻页与上下滑切换回答。阅读列居中限宽后，原本覆盖整栏的手势命中区域可能收缩到 720dp 内；如需保留全栏手势，应将相应手势处理置于全宽外层，并检查与正文滚动/点击的冲突。
- 不要用 `fillMaxWidth(0.6f)` 之类的比例限宽：窗口变宽后行宽会重新变长。

### P0-2 两栏层级

**目标文件**

- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/ZhihuMain.kt`（`DetailPane`，616 行起；分栏 Row 在 1073 行起）
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/ui/AdaptiveListDetail.kt`（`EmptyDetailPane`）

**做法**

先在浅色和深色主题下确认列表栏、空详情栏、实际详情页面分别绘制了什么背景；根据截图决定采用分栏边界/阴影、列表栏背景或详情页局部背景调整。注意：`EmptyDetailPane` **目前已经使用** `MaterialTheme.colorScheme.surface`，而文章/想法等详情页的 `Scaffold` 可能绘制不透明背景；仅给 `DetailPane` 外层增加 `surface` 未必产生可见效果。须验证空态和内容态颜色一致、层次确实可见。

**约束**

这一项属于观感调优。**不要**为了做出层次去批量修改各页 `Scaffold` 的 `containerColor`——那会改变所有页面的既有底色契约。优先尝试分隔条边界或列表栏 `surfaceContainerLow` 等局部调整，并通过实际截图验证；若必须改详情页背景，先明确跨页面的颜色契约及影响范围，避免仅修改外层背景却被内层覆盖。

### P1 分隔条桌面反馈

**目标文件**：`AdaptiveListDetail.kt`（`ListDetailDivider`，92 行起）

**做法**

- 加 `pointerHoverIcon(PointerIcon.Hand)`，让鼠标经过时变成可拖手势。
- 使用 `remember { MutableInteractionSource() }` 创建交互源，并在分隔条的热区 `Modifier` 上添加 `hoverable(interactionSource = interactionSource)` 以实际产生 `HoverInteraction`；再通过 `interactionSource.collectIsHoveredAsState()` 读取状态。**仅添加 `pointerHoverIcon()` 不会让该交互源更新 hover 状态。** hover 时中间线由 1dp 变 2dp、颜色由 `outlineVariant` 变 `primary`。
- 16dp 热区和现有 `draggable`、无障碍 `customActions` 保持不变。

**为什么只需 common API**：`hoverable`、`pointerHoverIcon` 与 `PointerIcon.Hand` 都有 Compose Multiplatform 的 common API；按照项目已使用的 Compose 版本确认 import 和编译结果，不需要为了此功能预设新增 `expect/actual`。

### P1 窗口状态记忆

**目标文件**：`desktopApp/src/main/kotlin/com/github/zly2006/zhihu/desktop/Main.kt`（34 行 `Window(...)`）

**现状**：`Window(title = "Zhihu-Hyperion", ...)` 没有传 `state`，Compose Desktop 每次启动都用默认 800×600 居中。

**做法**

- `rememberWindowState(size = DpSize(1360.dp, 860.dp), position = WindowPosition(Alignment.Center))` 作为首次启动默认值。**假设实际可分配给双栏的内容宽度为 1360dp**，扣除 16dp 分隔条后剩余 1344dp：按默认 1/3 比例，左栏 448dp、右栏 896dp；720dp 阅读列水平居中后，两侧各约 88dp 留白。实际结果须以内容区测量值为准，不能直接将窗口外框宽度视作内容宽度。
- 关闭时把宽、高、x、y、是否最大化写入 `SettingsStore`，启动时读回并 `coerceIn` 对齐到当前显示器可用区域（防止换显示器后窗口跑到屏幕外）。
- 键名常量放在桌面模块内（遵循 `CLAUDE.md`「偏好键常量应放在实际功能所属的设置模块」），不要塞进跨平台能力契约。

### P2（可选）空详情栏提示

`EmptyDetailPane` 现在只有图标和"选择左侧内容"。可补一行说明与常用快捷键提示。非必要，先不做。

## 5. 明确不做

- 不改 `:core:markdown` 的 `RenderMarkdown`。宽度应由页面容器决定，渲染器在 Android 上同样使用。
- 不拆分 `AppearanceSettingsScreen`(1976 行)、`PeopleScreen`(1879 行)、`CommentScreen`(1623 行)。纯重构、零用户可见收益，且与上游合并冲突。
- 不改 `canShowLandscapeListDetail` 的 840dp 门槛与 320/480dp 最小宽度，这些是既有契约。
- 不为了这次改动新增包装函数：`widthIn` + `align` 直接写在调用点。

## 6. 验收

- [ ] `./gradlew assembleLiteDebug` 通过
- [ ] `./gradlew ktlintFormat`，无额外格式改动
- [ ] Windows 实机：1360×860 与 1920×1080 各截一张双栏截图，确认行宽一致、正文居中
- [ ] 深色主题各截一张；同时分别验证空详情态和实际文章/想法页面的背景层级，确保调整确实可见且无跳色
- [ ] 回归：窗口窄于 720dp 时截图与改动前一致（手机/平板竖屏不受影响）
- [ ] 验证分隔条 hover 进入/退出时指针和线条反馈正常，拖动仍有效；拖到两端极限（列表 320dp / 详情 480dp），正文不破版、不出现横向滚动条
- [ ] 双栏下验证：垂直翻页、上下滑切换回答、横向滑切换回答、底部操作栏位置
- [ ] 重启应用，确认窗口尺寸/位置被保留

## 7. 提交拆分建议

按可独立回滚的最小单元拆分，PR 标题中文并以 `feat:` 开头：

1. `feat: 桌面双栏正文限宽与详情栏层级`
2. `feat: 桌面窗口尺寸与位置记忆`
3. `feat: 分隔条 hover 反馈`

第 1 项是本次问题的直接修复，建议单独成 PR 先行验证。涉及可见 UI，PR 描述必须附 1360 与 1920 两种窗口宽度的实际运行截图。
