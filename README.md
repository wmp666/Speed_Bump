<h4 align="right">
  简体中文 | <a href="README.en.md">English</a>
</h4>

<!-- LOGO -->
<div align="center">

<img src="src/com/wmp/speed_bump/common/background/resource/icon/dark/icon.png" alt="Speed Bump" width="120" />

## 减速带 · Speed Bump

**多线程下载器 · 可拓展更多其他功能**

[![Release][release-shield]][release-url]
[![License][license-shield]][license-url]
[![Stars][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Downloads][downloads-shield]][release-url]
[![Java][java-shield]](https://adoptium.net/)

##### [📖 使用文档][docs-url] · [⬇️ 下载安装][release-url] · [🐛 报告问题][issues-url] · [💡 功能建议][issues-url]

</div>

> [!TIP]
> 下载协议的实现细节、全部设置项、拓展开发与构建打包说明，都已整理到 [**helpDocs 文档中心**][docs-url]。

> [!TIP]
> 由于作者实力有限，该文档主要由AI编写，参考了其他项目的README<br>
有问题请反馈



<!-- FEATURES -->
## 功能特性

* **多协议** `HTTP/HTTPS` 多线程分段下载，服务器不支持分段时自动回落单线程；单线程断点续传、失败自动重试。`BT` 支持种子文件与磁力链接、可选文件下载、暂停与恢复；`ED2K` 等外部协议经拓展交由 `Gopeed` 处理。另有 `Github` 加速与 `gopeed://` 强制转发。
* **视频处理** 音视频**流拷贝合并**（不重新编码，速度最快）；容器与编码转码：`mp4 / mkv / avi / mov / flv / webm` × `h264 / h265` × `aac / mp3 / flac / wav`；自动探测本机 FFmpeg 能力并启用硬件加速。
* **可拓展** 一个 jar 就是一个拓展。内置 HTTP、BT、Gopeed、Github 解析器，官方拓展提供哔哩哔哩、抖音、ED2K、图片格式转换等能力；拓展页可查看、启用、禁用、卸载与一键安装 / 更新。
* **桌面原生体验** 系统托盘、失焦系统通知、拖拽添加任务与拖出文件、开机自启动、种子文件关联、背景图与主题 / 主题色、六种界面语言、单实例与命令行传参。

<!-- ABOUT -->
## 关于本项目

始于一个自用需求：既要能多线程把文件拉下来，也要能顺手处理音视频，还希望功能可以不断外挂进来——于是有了减速带，以及它的拓展体系。

| 平台 | 支持情况 |
|:--|:--|
| **Windows** | 完整支持 |
| **Linux** | 完整支持 |
| **macOS** | 完整支持（暂时无法推出相应版本） |

> [!Warning]
> 对于个平台的支持情况取决于Java 25，对这些平台的支持情况

| 项目 | 值                                                                      |
|:--|:------------------------------------------------------------------------|
| 当前版本 | `0.4.5.2`                                                               |
| 拓展开发版本 | `1.1.2`                                                                 |
| 运行环境 | `JDK 25+`                                                               |
| 拓展仓库 | [wmp666/Speed_Bump_Plugin](https://github.com/wmp666/Speed_Bump_Plugin) |
| 许可证 | [Apache License 2.0](LICENSE)                                           |

<!-- QUICK START -->
## 快速开始

1. 到 [Releases][release-url] 下载安装包：Windows 为 `Speed_Bump_Setup_*.exe`，Linux 为 `Speed_Bump_Setup_*.deb`
2. 启动后点「创建任务」，把链接粘贴进文本框（一行一个），**或直接把种子 / 图片等文件拖进文本框**
3. 任务卡片上可暂停、恢复、打开文件、打开所在文件夹；关闭任务时可选是否同时删除本地文件

需要自己编译？见 [构建与打包][build-doc]。

<!-- ROADMAP -->
## 计划

- [ ] 全局限速
- [ ] BT 种子、磁力链接任务的跨重启断点续传
- [ ] 关闭软件后保存未完成任务的进度，下次启动继续下载
- [ ] 将 UI 操作与后台操作完全分离

完整清单见 [功能状态][status-doc]，所有需求与已知问题见 [Issues][issues-url]。

<!-- DOCS -->
## 文档

| 文档 | 内容 |
|:--|:--|
| [文档中心][docs-url] | 全部文档的索引 |
| [使用指南](helpDocs/使用指南.md) | 下载功能、视频处理、界面与任务、设置项、数据与日志、命令行参数 |
| [拓展开发](helpDocs/拓展开发.md) | 拓展机制、`info.json` 规范、`AbstractParser` 接口、发布与分发 |
| [构建与打包][build-doc] | 环境要求、依赖、Windows / Linux 打包流程 |
| [功能状态][status-doc] | 已实现功能与待完善事项 |
| [鸣谢与声明](helpDocs/鸣谢与声明.md) | 第三方库、贡献者、开源许可与免责声明全文 |

<!-- CONTRIBUTING -->
## 提交你的想法

欢迎提交 Issue 与 Pull Request：

### 提交 Pull Request
1. Fork 本仓库
2. 新建分支（`git checkout -b feature/AmazingFeature`）
3. 提交改动（`git commit -m 'Add some AmazingFeature'`）
4. 推送到分支（`git push origin feature/AmazingFeature`）
5. 创建 Pull Request

提交 Bug 请尽量使用 [Bug 报告模板][bug-report-url]，附上日志（`~/.speed-bump/Log/`）与复现步骤。

<!-- LICENSE -->
## 许可证

基于 [Apache License 2.0](LICENSE) 分发。

本工具仅用于**个人学习、技术研究与学术交流**，请勿用于任何商业或侵权用途。完整免责声明见 [鸣谢与声明](helpDocs/鸣谢与声明.md#声明)。

<!-- MARKDOWN LINKS & IMAGES -->
[docs-url]: helpDocs/README.md
[build-doc]: helpDocs/构建与打包.md
[status-doc]: helpDocs/功能状态.md
[bug-report-url]: https://github.com/wmp666/Speed_Bump/issues/new?template=bug_report.yml
[release-shield]: https://img.shields.io/github/v/release/wmp666/Speed_Bump?style=for-the-badge
[release-url]: https://github.com/wmp666/Speed_Bump/releases/latest
[license-shield]: https://img.shields.io/github/license/wmp666/Speed_Bump?style=for-the-badge
[license-url]: LICENSE
[stars-shield]: https://img.shields.io/github/stars/wmp666/Speed_Bump.svg?style=for-the-badge
[stars-url]: https://github.com/wmp666/Speed_Bump/stargazers
[issues-shield]: https://img.shields.io/github/issues/wmp666/Speed_Bump.svg?style=for-the-badge
[issues-url]: https://github.com/wmp666/Speed_Bump/issues
[downloads-shield]: https://img.shields.io/github/downloads/wmp666/Speed_Bump/total?style=for-the-badge
[java-shield]: https://img.shields.io/badge/Java-25%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white
