# 减速带 · Speed Bump — Fluent 版（Speed_Bump_Fluent）

用 **Java + Swing** 实现的 **WinUI 3 / Fluent Design** 风格界面，是原项目
[`DownLoader`（减速带）](../README.md) 的 Fluent 重构版。原项目一行未动，本目录是完全独立的新工程。

> 本目录是**第一阶段**产物：Fluent 组件库 + 主框架骨架 + 可运行原型。
> 下载引擎（HTTP/BT/FFmpeg/拓展机制）尚未接入，见文末「下一步」。

---

## 一、先说清楚「Fluent UI / UWP」在 Java 里的边界

| 想要的东西 | 现实情况 |
|:--|:--|
| 真正的 **UWP / WinUI 3** | 只能 C# / C++ / XAML + WinRT，**Windows 独占**，Java 无法实现 |
| **Fluent Design 视觉与交互** | ✅ 完全可以用 Java 实现——本项目的目标就是这个 |
| Mica / Acrylic 材质 | ✅ JDK 的 FFM（`java.lang.foreign`）直接调 `dwmapi.dll` / `user32.dll`，零第三方依赖 |
| Segoe 字体与图标 | ✅ 直接用系统字体 `Segoe UI` / `Segoe MDL2 Assets`（图标即字体码位，无需打包图片） |

所以「Java 语言的 Fluent UI 项目」= **用 Java 复刻 WinUI 3 的观感与交互**，而不是换语言写 UWP。

技术选型为 **Swing + 自研 Fluent 组件库**（而非 JavaFX / Compose），理由是：
原项目已有 15k 行 Swing 代码与下载引擎，Swing 方案能最大程度复用逻辑层，
把改动集中在界面层——这正是「UI 与逻辑分离」最省力的落点。

---

## 二、快速开始

### 环境要求

| 项目 | 要求 |
|:--|:--|
| JDK | **22+**（FFM API 正式版）；实测 **Temurin 25.0.4** |
| 依赖 | `lib/` 下的 3 个 jar（FlatLaf 及 jsvg），已随目录提供 |
| 构建 | 只用 JDK 自带的 `javac` / `jar`，不需要 Maven / Gradle |

### 构建与运行

```powershell
# Windows PowerShell 5.1（系统自带）或 PowerShell 7 均可
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1          # 编译到 out/classes
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Clean   # 清理重编
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Jar     # 另出可执行 jar

powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1            # 运行
```

> 脚本带 UTF-8 BOM。**没有 BOM 的话，Windows PowerShell 5.1 会按 ANSI 解码文件**，
> 中文注释会被拆成乱码并导致语法错误——这个坑在迁移脚本时很容易再踩一次。

### 离屏自检（推荐先跑这个）

```powershell
java "--enable-native-access=ALL-UNNAMED" -Dfile.encoding=UTF-8 `
     -cp "out\classes;lib\*" com.wmp.speedbump.fluent.FluentApp --selftest --dump-tree
```

自检会在**不显示窗口**的前提下完成：装配整套界面 → 布局 → 离屏绘制 → 校验 → 出报告：

| 产物 | 内容 |
|:--|:--|
| `out/selftest-light.png` / `-dark.png` | 明暗两套主题的完整界面截图，可直接肉眼复核 |
| `out/selftest-*-tree.txt` | 组件树 + 每个组件的实际坐标尺寸 |
| `out/selftest-report.txt` | UTF-8 报告（含环境、字体、材质路径与全部校验结论） |

它检查四类问题，全部是 GUI 项目里最容易悄悄坏掉的地方：

1. **装配**：组件树能不能建起来（异常会被捕获成报告里的一条）；
2. **布局**：标题栏高度、内容区尺寸、以及「可见但宽或高为 0」的组件；
3. **像素**：强调色按钮、进度条填充、导航选中指示条的实际像素颜色是否符合令牌；
4. **区域复杂度**：标题栏 / 导航窗格 / 内容区的非底色像素占比，
   低于阈值说明「那一块根本没画出来」。

### 命令行参数

| 参数 | 说明 |
|:--|:--|
| `--theme=light\|dark\|system` | 起始主题 |
| `--material=acrylic\|mica\|mica-alt\|blur\|none` | 窗口材质 |
| `--accent=#RRGGBB` | 强调色 |
| `--no-backdrop` | 关闭原生材质 |
| `--no-reveal` | 关闭揭示高亮光晕 |
| `--static` | 关闭演示动画（截图对照用） |
| `--verbose` | 输出启动分步耗时（排查启动慢时先看它） |
| `--selftest` / `--dump-tree` | 离屏自检 / 追加组件树转储 |
| `--screenshot=<目录>` | 自检产物输出位置 |

---

## 三、目录结构

```
Speed_Bump_Fluent/
├── build.ps1 / run.ps1            构建与运行脚本（UTF-8 BOM）
├── build.xml                      Ant 构建（可选，与原项目习惯保持一致）
├── Speed_Bump_Fluent.iml          IntelliJ 模块
├── lib/                           FlatLaf 3.7.2 / flatlaf-extras / jsvg
└── src/com/wmp/speedbump/fluent/
    ├── FluentApp.java             入口 + 命令行参数 + 离屏自检
    ├── theme/                     ★ 令牌层：颜色 / 字体 / 尺寸 / 图标 / 绘制工具
    ├── backdrop/                  ★ 原生材质：FFM 直调 DWM 与 user32
    ├── reveal/                    ★ Reveal Highlight 光晕引擎
    ├── component/                 ★ Fluent 组件库（10 个控件）
    ├── ui/                        ★ 框架：无边框窗口 / 标题栏 / 导航视图 / 页面基类
    └── page/                      三个页面原型：任务 / 创建任务 / 设置
```

设计原则是**单向依赖**：
`page → ui → component → theme`，以及 `ui/theme → backdrop`。
令牌层不依赖任何上层，因此换主题只需要动 `theme` 包。

---

## 四、已实现清单

### 框架（`ui`）

- **`FluentWindow`** —— 逐像素透明的无边框窗口：标题栏拖动、双击最大化、边缘 6px 缩放（全局鼠标监听）、
  最大化到**工作区**（不会盖住任务栏）、初始尺寸自动收敛到屏幕内、材质失败自动退化。
- **`FluentTitleBar`** —— 32px 标题栏，最小化 / 最大化 / 关闭按钮用**矢量绘制** 10×10 线条
  （不用图标字体，任何 DPI 都锐利，也不会因为缺字体变方块），关闭按钮悬停变 Windows 红。
- **`FluentNavigationView`** —— 左侧导航：选中指示条（3×16 主题色圆角竖条）、项目圆角底色、
  展开 280px / 折叠 48px、顶部汉堡按钮与搜索框、底部固定项。
- **`FluentPage`** —— 页面基类：固定标题区 + 可滚动内容列，内容列宽度上限 1080px 并居中。

### 组件库（`component`）

| 组件 | 要点 |
|:--|:--|
| `FluentButton` | Standard / Accent / Subtle / Hyperlink 四样式，图标+文字整体居中，按下内容下移 1px |
| `FluentToggleSwitch` | 40×20 胶囊轨道 + 滑块 150ms 位移动画，文字在左、开关在右（WinUI 布局） |
| `FluentCheckBox` | 选中填充主题色 + 对勾字形；无图标字体时退化为矢量折线 |
| `FluentRadioButton` | 与 `ButtonGroup` 天然兼容 |
| `FluentTextBox` | 外壳负责圆角底与「聚焦变主题色的 2px 底线」，内层 `JTextField` 负责输入 |
| `FluentTextArea` | 同上，多行版，占位提示自绘 |
| `FluentComboBox` | 复用 FlatLaf 的圆角底与箭头，补一条 WinUI 底线 |
| `FluentCard` | 半透明卡面 + 1px 淡描边；`addRow(标题, 说明, 控件)` 一行搞定设置项 |
| `FluentProgressBar` | 4px 全圆角，支持不确定态扫动动画 |
| `FluentScrollPane` | 悬浮细滚动条：常态 3px、悬停展开 8px |

### 引擎（`theme` / `backdrop` / `reveal`）

- **颜色令牌** `FluentColors`：约 25 个语义令牌，浅/深两套取值，支持自定义强调色。
- **字体令牌** `FluentTypography`：`Segoe UI Variable` → `Segoe UI` → 逻辑字体 三级降级。
- **图标令牌** `FluentIcons`：`Segoe Fluent Icons` → `Segoe MDL2 Assets`，全是字体码位。
- **原生材质** `NativeBackdrop`：Mica / Mica Alt / Acrylic / Blur / 无，FFM 直调，零第三方依赖。
- **注册表读取** `WindowsRegistry`：FFM 直调 `RegGetValueW`，用于「跟随系统主题」，不到 1 毫秒。
- **揭示高亮** `RevealEngine`：Fluent 的标志性光晕，全局指针模型 + 60FPS 插值 + 渐变淡入淡出。

### 页面（`page`）

- **任务**：工具栏 + 概览统计卡 + 任务卡片（文件名 / 状态徽标 / 进度条 / 详情 / 四个操作按钮），
  带演示动画。
- **创建任务**：多行链接输入、保存位置、协议与线程数下拉、断点续传开关、下载后处理单选组。
- **设置**：**这里的开关是真能用的**——主题、材质、强调色、光晕开关会即时改变界面，
  并附「环境诊断」卡片，把材质与字体的实际可用性写在界面上。

---

## 五、五个必须知道的坑（都已在本项目里解决）

### 1. Mica 在 Swing 窗口上会「假成功」

Swing 的逐像素透明窗口在 Win32 层面是 **layered 窗口**，而 DWM 的 Mica/Acrylic
要求**非 layered** 窗口才会真正渲染。要命的是：在 layered 窗口上
`DwmSetWindowAttribute(SYSTEMBACKDROP_TYPE)` **返回成功**却什么都不画——
于是任何「失败就回退」的代码都不会触发，表现为「材质开了但没效果」。

**处理**：`NativeBackdrop.forceBlurInsteadOfMica` 默认为 `true`，强制走
`SetWindowCompositionAttribute` 的模糊路径（Windows 10 1803+ 与 Windows 11 都真的有效）。

### 2. 材质应用得太早会永久退化为不透明

`applyBackdrop()` 需要在 `setVisible(true)` **之后**执行（窗口必须先实现），
但此时窗口还没进入可见状态，`EnumWindows + IsWindowVisible` 枚举不到它，
拿到的句柄是 0，于是被判定为「不支持材质」，**整个进程**退化成不透明背景。

**处理**：改为「乐观 + 轮询重试」（60ms × 25 次）。只有全部失败才真正退化，
成功与失败都会打日志，便于排查。这个 bug 是靠真实窗口日志里的
`材质生效: false` 抓出来的。

### 3. 固定尺寸窗口在小屏上会超出屏幕

实测屏幕 1366×768，而默认窗口 1200×780——**比屏幕还高 12px**，
窗口底部的操作区永久落在屏幕之外。

**处理**：初始尺寸收敛到 `getMaximumWindowBounds()`（排除任务栏的工作区）减去留白。

### 4. 用子进程读注册表会让界面冻结十几秒

「跟随系统主题」要读 `HKCU\...\Personalize\AppsUseLightTheme`。最初的实现是
`new ProcessBuilder("reg", "query", ...)`，实测一次要 **11.7 秒**——而设置页的选项
在 EDT 上同步调用它，用户一切到「跟随系统」界面就毫无提示地冻住。

**处理**：改为 FFM 直调 `advapi32!RegGetValueW`（`WindowsRegistry`），耗时降到 **<1ms**。
这一步从 11731ms 变成 891ms（后者是必须重装 FlatLaf 深色主题的真实开销）。

> 附带教训：`ProcessBuilder` 不要「先 `waitFor(超时)` 再读 stdout」——
> 子进程输出填满管道缓冲区时会死锁，而且那个超时值是拍脑袋定的。
> 正确做法是读到 EOF（读空即子进程说完），必要时再 `waitFor` 一个很短的兜底时间。

### 5. 主题监听器用强引用列表会泄漏

`FluentTheme.addListener(this::repaint)` 这种写法有两个坑：组件销毁后回调仍留在列表里；
而简单改成弱引用回调又会**静默失效**（`this::repaint` 每次都是新对象，除列表外无人强引用它）。

**处理**：把**组件本身**当弱键放进 `WeakHashMap`，值用共享的 `Boolean.TRUE`
（值不反向引用键，条目才能在组件回收后过期），组件通过实现
`FluentTheme.ThemeAware` 接收通知。

---

## 六、性能

`--verbose` 会输出启动分步耗时。本机（Windows 10 19044 / JDK 25 / 虚拟机）实测的典型构成：

| 阶段 | 耗时 | 说明 |
|:--|:--|:--|
| 安装 Look & Feel 与令牌 | 2.1 – 4.0 s | `FlatLaf.setup()` 首次装载整套 UI 默认值，**冷 JVM 占大头** |
| 切换主题（仅明暗变化时） | 0.07 s | 明暗没变则完全跳过重装 |
| 切换主题（需换 L&F，如跟随系统→深色） | 0.9 s | 确实要重装一次 FlatLaf |
| 创建窗口骨架 | 1.8 – 2.7 s | 无边框透明窗口的 peer 创建 + 标题栏 |
| 构建首页 | 0.5 – 1.5 s | 组件构造 + 字体首次度量 |
| 注册其余页面 | 0.15 s | 延迟构建，不再是 1.5 s |
| 显示窗口（含材质应用） | 0.8 – 1.7 s | 透明窗口 + DWM 调用 |

冷启动合计约 **7 – 10 秒**。已做的优化：

1. **明暗未变就不重装 L&F**（`FlatLaf.setup()` 一次 1–2 秒）；
2. **页面延迟构建**——只有首页立即构造，另两页首次点开才建；
3. **字体实例缓存**——`FluentTypography.body()` 等改为返回静态常量，避免大量重复构造；
4. **注册表改 FFM 直读**，去掉 11 秒的子进程。

仍可优化但尚未做的：`BoxLayout` 嵌套下首选尺寸被反复计算
（`FluentCard.getMaximumSize()` 会触发整棵子树的 `getPreferredSize()`），
彻底解决需要给容器加首选尺寸缓存与失效机制——收益有限、出错风险不低，留待后续。

---

## 七、下一步（第二阶段）

当前阶段刻意只做界面，下载引擎一律未接。继续推进的顺序建议：

| 步骤 | 内容 | 依赖 |
|:--|:--|:--|
| 1 | 把 `com.wmp.downloader.tools.*` 整体搬进本工程，包名改为 `com.wmp.speedbump.*` | 无界面改动 |
| 2 | 任务数据模型接上 `TasksPage.TaskCard`（把 `setProgress` 接到真实任务对象） | 步骤 1 |
| 3 | 创建任务页接上真实下载引擎，SettingsPage 接上配置持久化 | 步骤 1 |
| 4 | 托盘菜单、系统通知、拖拽、单实例、多语言（复用原项目 `laug_*.properties`） | 步骤 2 |
| 5 | 拓展机制（`AbstractParser` + `info.json`）与内置解析器 | 步骤 2 |
| 6 | 打包脚本（Inno Setup / deb）与图标资源 | 全部 |

组件库、主题令牌与窗口框架在本阶段已经定型，后续步骤基本不需要再改它们。

---

## 八、许可

与原项目一致：[Apache License 2.0](../LICENSE)。
