# 减速带 (Speed Bump)

一个用 Java 写的**多线程下载器 + 视频处理工具箱**。

1. 多线程 + 单线程下载工具，支持 HTTP、HTTPS、BT（Torrent、Magnet），并通过拓展接入 ED2K 等更多协议
2. 视频处理器：支持合并**音频、视频**；更改视频格式与编码
3. 下载器，但是不只是下载器——通过**拓展（插件）机制**持续挂载更多实用功能

| 项目     | 值                                                                      |
| ------ | ---------------------------------------------------------------------- |
| 当前版本   | `0.4.5.2`                                                              |
| 拓展开发版本 | `1.1.2`                                                                |
| 主程序仓库  | <https://github.com/wmp666/Speed_Bump>                                 |
| 拓展仓库   | <https://github.com/wmp666/Speed_Bump_Plugin>                          |
| 许可证    | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html) |

---

## 目录

- [一、下载能力](#一下载能力)
- [二、视频处理](#二视频处理)
- [三、任务与界面](#三任务与界面)
- [四、设置项](#四设置项)
- [五、拓展（插件）](#五拓展插件)
- [六、数据与日志](#六数据与日志)
- [七、命令行与单实例](#七命令行与单实例)
- [八、构建与打包](#八构建与打包)
- [九、功能状态](#九功能状态)
- [十、鸣谢](#十鸣谢)
- [声明](#声明)

---

## 一、下载能力

### HTTP / HTTPS

- **多线程分段下载**：把文件按线程数切成等长分段，每段写入临时目录中的 `N.part`，全部完成后顺序合并为最终文件；每段独立进度条 + 总进度/速度显示
- **自动降级为单线程**：先用 `HEAD` 探测 `Accept-Ranges` 与文件大小，服务器不支持分段或拿不到大小时自动改走单线程，不会硬失败
- **单线程断点续传**：单线程模式通过 `Range: bytes=N-` 从已下载长度继续；分段模式按各 `part` 文件长度续传，已完成的段会被跳过
- **失败重试**：单线程与每个分段最多重试 10 次，重试间隔递增；重试仍失败则标记该段失败并清理分段文件
- **文件名解析**：优先使用 `Content-Disposition`（含 `filename*` 的 UTF-8 形式），其次取 URL 路径末尾，最后回退为默认名
- **覆盖与大小校验**：本地已存在同名文件会先询问；本地文件大于目标大小（疑似残留错误文件）时会提示并删除后重下
- **线程数可调**：默认 64，可在设置页或创建任务时用滑条调整

### BT（Torrent / Magnet）

- 基于 [frostwire-jlibtorrent](https://github.com/frostwire/frostwire-jlibtorrent)（2.0.12.9）
- 支持 **`.torrent` 种子文件**（把文件拖入创建任务的链接框即可）与 **`magnet:` 磁力链接**
- 磁力链接会先通过 libtorrent 获取元数据、生成临时种子文件，再进入解析与下载流程
- **多文件种子可选文件下载**：解析出文件列表后，可选择要下载哪些文件（未选中的文件以 `IGNORE` 优先级加入），并实时计算选中体积
- 任务支持暂停 / 恢复（libtorrent `pause` / `resume`）；多文件（文件夹）任务在暂停或关闭时会保存 resume data
- 显示下载量、下载速度与分享量

### Gopeed 与 ED2K

- 内置 Gopeed 解析器：通过 Gopeed 本地 HTTP API（默认 `http://127.0.0.1:9999/api/v1`）创建/暂停/继续/删除任务，并读取其进度、下载速度、上传量与上传速度
- 触发方式：
  - 链接以 **`gopeed://`** 开头，强制把后续地址交给 Gopeed
  - 由拓展（如 ED2K 解析器）把链接交给 Gopeed —— 主程序自身不含 ED2K 协议栈
- 未检测到 Gopeed 时会询问是否自动启动（使用专项设置中配置的程序路径），两端端口不一致会明确报错
- Gopeed 的程序路径与端口在「专项设置 → Gopeed 设置」中配置

### Github 加速

- `github.com` 链接会改写到加速站下载（内置 `gh-proxy.com`、`gh-proxy.org`，也支持自定义加速站）
- 加速站配置同时用于**软件更新检查**与**拓展安装列表**的获取
- 相关设置在「专项设置 → Github 加速」

### 其它下载行为

- 系统代理：遵循系统代理设置（`java.net.useSystemProxies`）
- SSL：可在设置中关闭证书校验，用于处理自签名/证书异常的环境
- 下载中可随时「全部暂停 / 全部启动」，单个任务可独立暂停、恢复、关闭
- 关闭任务时可选择**同时删除本地文件**（该选择会被记住）

---

## 二、视频处理

创建任务界面的「视频处理」页提供两类任务：

### 音视频合并

- 选择**视频文件** + **音频文件** + 输出文件名，合并为完整文件
- 使用 FFmpeg **流拷贝**（`-c copy`，`-map 0:v:0` / `-map 1:a:0`，并加 `+faststart`），不重新编码，速度最快
- 合并过程不可暂停，任务卡片会显示合并状态

### 转码 / 转换格式

- 可选输出容器：`mp4`、`mkv`、`avi`、`mov`、`flv`、`webm`
- 可选视频编码：`h264 (AVC)`、`h265 (HEVC)`
- 可选音频编码：`aac`、`mp3`、`flac`、`wav`
- 适合把不可播放的格式/编码改成播放器更友好的组合

### FFmpeg 与硬件加速

- 合并与转码都通过调用 FFmpeg 完成：优先使用「专项设置 → FFmpeg 设置」中指定的本地 FFmpeg，未指定时回退到系统 `PATH` 中的 `ffmpeg`
- **硬件加速**（默认开启）：自动执行 `ffmpeg -hwaccels` 与 `ffmpeg -encoders` 探测本机能力，按 `cuda/nvdec → qsv → vaapi → vdpau → videotoolbox` 的优先级选择解码加速与硬件编码器（`h264_nvenc`、`h264_qsv`、`h264_vaapi`、`h264_amf`、`h264_videotoolbox` 等），检测不到则退回软件编码
- 检测结果会缓存，避免每次转码重复探测

---

## 三、任务与界面

- **创建任务**：把链接粘贴/输入到文本框（一行一个），也可**直接把文件拖入文本框**（自动填入文件路径，用于种子、图片处理等以路径为输入的拓展）；链接会被增量解析并实时生成任务信息卡片
- **任务卡片**：显示文件/文件夹名、大小、模式（协议/解析器）、进度条、实时速度；提供打开文件、打开所在文件夹、暂停/恢复、关闭
- **拖出文件**：已完成的任务卡片可直接拖拽到文件管理器，得到真实文件
- **主窗口页面**：任务中心、通用设置、专项设置、拓展、关于
- **状态栏**：左下角显示「正在运行 N 个任务 · 共 M 个任务」并可点击回到任务中心；右下角是 **工具箱**（把应用菜单原样转成卡片）与 **消息中心**
- **消息中心**：所有 Toast 通知会留档（按日期分组、最多保留 20 条），可随时回看
- **系统托盘**：关闭窗口即最小化到托盘，托盘菜单可显示窗口或退出，双击托盘图标唤出主界面
- **窗口失焦系统通知**：窗口不在前台时，通知会同时通过系统托盘弹出（可在设置中开关）
- **背景与主题**：支持设置背景图与透明度（Alpha）、主题、主题色、字体与字号、方角/圆角组件
- **多语言**：简体中文、繁體中文（臺灣 / 香港地區）、English、日本語、Русский язык（切换后重启或重载窗口生效）
- **开机自启动**：Windows 写注册表 `HKCU\...\Run`，macOS 写 `~/Library/LaunchAgents` 的 LaunchAgent，Linux 写 `~/.config/autostart` 的 desktop 文件；自启动时带 `-background` 参数静默启动
- **文件关联**：可把 `.torrent` 关联到本程序（Windows 注册表 / macOS `lsregister` / Linux `mimeapps.list` + MIME 数据库，均只作用于当前用户）
- **检查更新**：从 GitHub Releases 获取新版本，按平台自动选择 `Speed_Bump_Setup*.exe` / `.dmg` / `.deb`；可在启动时自动检查；更新日志支持用微软翻译（需自行在首次使用时填写 Azure Translator 密钥与区域）翻译后再阅读
- **测试项管理**：部分实验性功能默认隐藏，可在「软件 → 测试」中逐项启用（窗口重载按钮、更新日志翻译按钮、窗口置顶、剪切板监听设置项等）
- **剪切板监听**：开启后轮询剪贴板（500ms），识别到 `BV*` / `bilibili.com` / `http(s)` 链接并做可达性校验后，自动弹出创建任务窗口并填入链接（默认关闭，设置项需先在测试项管理中启用）

---

## 四、设置项

### 通用设置

| 分类     | 设置项                                                                                  |
| ------ | ------------------------------------------------------------------------------------ |
| 网络设置   | 是否使用 SSL（关闭证书校验）                                                                     |
| 下载设置   | 线程数、下载位置、缓存数据路径、是否监听剪切板                                                              |
| 个性化    | 主题、背景图与 Alpha、主题色、字体、默认字体大小、使用方角组件、功能弹窗样式（嵌入式 / 本地化嵌入式 / 独立弹窗）、其他（开机自启动、失去焦点时使用系统通知） |
| 文件关联设置 | 种子文件（`.torrent`）的关联 / 取消关联                                                           |
| 数据管理   | 打开数据文件夹、打开下载文件位置、删除下载缓存                                                              |

### 专项设置

专项设置页由**各解析器/拓展自己提供**，当前包含：

- **Github 加速**：启用开关、加速站选择（`gh-proxy.com` / `gh-proxy.org` / 自定义）
- **Gopeed**：程序路径、端口
- **FFmpeg**：本地 FFmpeg 路径、是否使用硬件加速
- 拓展自带的设置页（例如哔哩哔哩扫码登录与默认画质）

---

## 五、拓展（插件）

### 什么是拓展

拓展就是**一个 jar 包**，其中包含若干继承 `AbstractParser` 的解析器。解析器负责把某种输入（链接或文件路径）解析成一张任务信息卡片，并据此创建下载/处理任务。所有下载与处理能力都统一挂在解析器体系下：HTTP、BT、Gopeed、Github 是内置解析器，其余能力都以拓展形式装载。

- 拓展目录：`~/.speed-bump/Data/ParsersATools/*.jar`
- 拓展页支持：查看**已安装**列表、启用 / 禁用、卸载（重启后完全生效）、导入本地 jar、刷新；以及从**拓展仓库的 Releases** 拉取可安装列表并一键安装 / 更新
- 内置的 Github 加速以「内置插件」形式出现，在列表中只读、不可卸载

### 官方拓展（由拓展仓库提供）

| 拓展        | 说明                                                                         |
| --------- | -------------------------------------------------------------------------- |
| 哔哩哔哩链接解析器 | 支持大部分哔哩哔哩网页链接 / BV 号（视频、视频合集），不支持多线程下载                                     |
| 抖音分享链接解析器 | 解析抖音分享链接（视频、图集），依赖第三方 API，不支持多线程下载                                         |
| ED2K 解析器  | 解析 ED2K 链接，实际下载由 Gopeed 完成                                                 |
| 图片格式转换器   | 读取 30+ 种图片格式，可写出 jpg / png / bmp / gif / tiff / ico / icns 等格式（把图片拖入链接框即可） |
| 每日金句      | 通过系统托盘通知，在启动时或每日固定时间点推送一句金句                                                |

### 自己写一个拓展

1. 新建项目，把本仓库作为项目库（**不要把主程序的类打进插件 jar**，主程序类必须由父类加载器解析）

2. 在项目**根目录**编写 `info.json`：
   
   ```json
   {
     "id": "我的解析器",
     "mainClass": "com.example.MyParser",
     "version": "1.0.0",
     "author": "你的名字",
     "plugin_support_version_start": "1.1.0",
     "plugin_support_version_last": "+",
     "support_platform": "all"
   }
   ```

3. （可选）根目录编写 `introduction.md` 作为拓展介绍（原生 `JEditorPane` 渲染，Markdown 支持有限）

4. 解析器继承 `AbstractParser`，实现：`getID()`、`getSupportTip()`、`isMeetRequirements(String)`、`getSettingsPage()`、`updateLinkInfo(String)`、`getLinkedInfoPanel(String, Info)`、`getTask(String, JSONObject)`

5. 面板与任务之间的数据通过 `AbstractLinkInfoPanel#getJsonInfo()` 产生的 `jsonInfo` 传递，任务侧再从构造参数中取出

要点：

- **开发版本**只在修改了与拓展加载相关的代码时变化：大版本（`a`）表示完全不兼容、需要重做拓展；小版本（`b`）表示新增功能、建议同步更新
- 拓展开发版本必须落在 `plugin_support_version_start ~ plugin_support_version_last` 区间内，`+` 表示无穷；不匹配时启动会弹出版本警告
- `support_platform` 仅用于安装提示（`all` / `windows` / `linux` / `mac`，留空视为全平台）
- 拓展通过拓展仓库的 **GitHub Release** 分发：Release 标题与 Tag 使用拓展名，Body 按 `### 信息`（name / author / version / plugin_support_version / support_platform）与 `### 介绍` 两节书写，并挂上插件 jar 作为 asset

更完整的说明见 [`拓展解析器制作.md`](拓展解析器制作.md)、[`installPlugin.md`](installPlugin.md) 与 [`应用商店拓展实现方案.md`](应用商店拓展实现方案.md)（社区提交的「纯 jar 应用商店」设计方案，尚未并入主程序）。

---

## 六、数据与日志

所有数据都保存在用户目录下的 `~/.speed-bump`：

| 路径                            | 内容             |
| ----------------------------- | -------------- |
| `Data/data.json`              | 全部设置项（键值对）     |
| `Data/Parser.json`            | 拓展的禁用列表与待删除列表  |
| `Data/ParsersATools/`         | 已安装的拓展 jar     |
| `Data/msgsInfo.json`          | 消息中心的通知记录      |
| `Data/testEnableList.json`    | 已启用的测试项        |
| `Data/file_icon/`             | 文件关联时释放的图标     |
| `Data/Download/`              | 默认下载目录（可改）     |
| `Log/app.log`、`Log/error.log` | 按天滚动的运行日志与错误日志 |

缓存分片与临时种子文件默认放在系统临时目录的 `speed-bump` 下，可在设置中改为自定义路径或一键清除。

---

## 七、命令行与单实例

程序同一时间只应有一个实例运行，它会在 `127.0.0.1:5465`（端口可配置，重启生效）监听本地消息，用于接收其它实例转交的链接：

```text
SpeedBump.exe "https://example.com/file.zip"   # 已运行时：把链接转交给已有窗口
SpeedBump.exe -background                      # 启动后不显示主窗口（开机自启动使用）
SpeedBump.exe -noUseTCPServer                  # 不启用本地通信服务
SpeedBump.exe -set:version 0.4.5.2             # 覆盖显示的版本号（调试用）
```

把 `.torrent` 文件或任意资料链接作为第一个参数传入，即可由已运行的实例直接弹出创建任务窗口。

---

## 八、构建与打包

- **JDK 21+**（使用了虚拟线程、`record`、模式匹配等特性）

- 依赖 jar 位于 `lib/`，工程为 IntelliJ IDEA 项目（`DownLoader.iml`），入口类 `com.wmp.downloader.Run`

- `lib/` 中提供 jlibtorrent 的 Windows 与 Linux（x86_64 / arm64）原生库；其它平台需要自行补充对应原生库，否则 BT 功能不可用

- 产物目录 `out/`：
  
  - Windows：IDEA 导出 `DownLoader_Windows_jar` → exe4j 生成 `SpeedBump.exe` → Inno Setup 脚本 `out/减速带.iss` 生成安装包
  - Linux：用 `jpackage` 生成 deb，必要时再用 `dpkg-deb --build --root-owner-group` 重新打包
  
  ```bash
  jpackage --input /path/to/SpeedBump --main-jar DownLoader_Linux.jar \
           --main-class com.wmp.downloader.Run --icon /path/to/icon.png \
           --name SpeedBump --type deb --dest /path/to/JarDebs
  
  dpkg-deb --build --root-owner-group /path/to/package /path/to/JarDebs/Speed_Bump_Setup_<版本>.deb
  ```

- `lib/` 中还放有 `java-youtube-downloader`，目前未接入主流程

---

## 九、功能状态

### 已实现

- [x] HTTP / HTTPS 单线程 + 多线程分段下载，单线程断点续传
- [x] BT 种子与磁力链接下载，多文件选择、暂停 / 恢复
- [x] Gopeed 联动（`gopeed://` 链接与 ED2K 等外部协议）
- [x] Github 加速
- [x] 拓展机制：安装 / 更新 / 启用 / 禁用 / 卸载 / 本地导入
- [x] 视频合并（流拷贝）与转码（容器 / 视频编码 / 音频编码可选）
- [x] FFmpeg 硬件加速自动检测
- [x] 拖入文本域（创建任务页）添加文件路径
- [x] 已下载任务拖拽导出文件
- [x] 关闭任务时可选删除本地文件
- [x] 剪切板监听（需在测试项管理中启用）
- [x] 失去焦点时使用系统通知
- [x] 开机自启动、种子文件关联
- [x] 背景图与 Alpha、主题 / 主题色 / 字体、多语言
- [x] 关闭 SSL、系统代理
- [x] 单实例与命令行传参

### 待完善 / 计划中

- [ ] 全局限速
- [ ] BT 种子、磁力链接任务的跨重启断点续传
- [ ] 关闭软件后保存未完成任务的进度，下次启动继续下载（已下载内容仍留在任务列表）
- [ ] 将 UI 操作与后台操作完全分离
- [ ] Gopeed 使用教程
- [ ] 应用商店（现仅有社区设计方案文档）
- [ ] ……

---

## 十、鸣谢

### 第三方库

- [JFormDesigner:FlatLaf](https://www.formdev.com/flatlaf)（含 flatlaf-extras、flatlaf-swingx）
- [SwingX](https://github.com/arotenberg/swingx)
- [alibaba:fastjson](https://github.com/alibaba/fastjson)（项目实际使用 fastjson2）
- [google:zxing core+javase](https://github.com/zxing/zxing)（哔哩哔哩扫码登录）
- [org.jsoup:jsoup](https://jsoup.org/)
- [log4j:log4j](https://logging.apache.org/log4j/2.x/index.html)
- [io.github.dj.raven:modal-dialog](https://github.com/DJ-Raven/swing-modal-dialog)
- [frostwire:frostwire-jlibtorrent](https://github.com/frostwire/frostwire-jlibtorrent)
- [commons.net](https://commons.apache.org/proper/commons-net/)
- [org.commonmark](https://github.com/commonmark/commonmark-java)

### 贡献者

1. 作者：开发者
2. 工具：AI——减少了不必要的重复工作
3. [Karagarasu](https://github.com/Karagarasu)：提供功能建议

### 其他

- [Ghost Downloader 3](https://github.com/XiaoYouChR/Ghost-Downloader-3/releases)：提供 **UI** + **功能** 启发
- [遇见API](https://api.yujn.cn)：提供部分API支持
- [Nieobie:game-icon-pack](https://github.com/Nieobie/game-icon-pack)：提供图标
- [gopeed](https://gopeed.com/)

---

# 声明

**本工具**（以下简称“**本软件**”）仅用于 **个人学习、技术研究和学术交流** 之目的，旨在帮助用户了解视频平台的数据传输机制与文件格式。
用户在使用本软件下载任何视频内容前， **必须** 仔细阅读并同意以下条款：

1. **版权归属**
   所有通过本软件下载的视频、音频、封面图等内容的版权均归原始权利人所有。本软件**不占有、不修改、不转授**任何下载内容的版权。
2. **合法使用承诺**
   用户承诺仅下载 **自己拥有合法授权** 或 **已获权利人明确许可** 的内容，或下载用于 **合理引用、解说、学术研究** 等符合《中华人民共和国著作权法》第二十四条规定的“合理使用”情形。
3. **禁止行为**
   - 严禁 将下载内容用于以下用途：
     1. 商业盈利、广告投放、付费分发或任何形式的变现；
     2. 篡改水印、冒名发布、侵犯原作者署名权；
     3. 批量抓取、数据爬取或破坏平台正常运营秩序；
     4. 传播违法信息、低俗内容或侵犯他人肖像权、隐私权；
     5. 其他违反国家法律法规、平台服务协议及公序良俗的行为。
4. **责任承担**
   用户因违反上述条款而产生的 **全部法律责任（包括但不限于民事赔偿、行政处罚、平台追责等）均由用户自行承担** ，与本软件开发者、运营者及贡献者无关。本软件不提供任何内容上的担保，亦不对下载后内容的完整性、合法性做任何明示或默示的保证。
5. **终止与删除**
   若本软件收到相关权利人的有效侵权通知，开发者有权随时终止服务或屏蔽特定功能。用户下载的内容应在 **学习完成后24小时内删除** ，不得长期留存。
   特别提醒：请尊重每一位创作者的劳动成果。若您希望长期欣赏或使用某作品，请前往官方平台进行正版观看或购买授权。
   使用本软件即视为您已阅读、理解并同意本免责声明全文。若不同意，请立即停止使用并卸载本软件。
   （本声明最终解释权归本软件开发者所有，并保留根据法律法规变化适时修订的权利。）
