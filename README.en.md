<h4 align="right">
  <a href="README.md">简体中文</a> | English
</h4>

<!-- LOGO -->
<div align="center">

<img src="src/com/wmp/speed_bump/common/background/resource/icon/dark/icon.png" alt="Speed Bump" width="100" />

## Speed Bump

**Multi-threaded downloader · Can expand to more other functions**

[![Release][release-shield]][release-url]
[![License][license-shield]][license-url]
[![Stars][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Downloads][downloads-shield]][release-url]
[![Java][java-shield]](https://adoptium.net/)
[![Platform][platform-shield]][build-doc]

##### [📖 Documentation][docs-url] · [⬇️ Download][release-url] · [🐛 Report a bug][issues-url]

</div>

> [!TIP]
> Detailed docs (download internals, every setting, plugin development, build & packaging) live in the [**helpDocs**][docs-url] (Chinese only).

<!-- FEATURES -->
## Features

* **Protocols.** Multi-threaded segmented `HTTP/HTTPS` downloads that fall back to a single connection when the server rejects ranges; resume support and automatic retries. `BT` torrent files and magnet links with per-file selection, pause and resume. External protocols such as `ED2K` are handled by `Gopeed` through plugins, plus `Github` acceleration.
* **Video toolkit.** Audio/video **stream-copy merging** (no re-encoding) and transcoding across `mp4 / mkv / avi / mov / flv / webm` × `h264 / h265` × `aac / mp3 / flac / wav`, with automatic FFmpeg hardware-acceleration detection.
* **Extensible.** One jar equals one plugin. HTTP, BT, Gopeed and Github parsers are built in; official plugins add Bilibili, Douyin, ED2K and image format conversion. Manage, enable, disable and update plugins in-app.
* **Native desktop feel.** Tray icon, system notifications when unfocused, drag & drop tasks and files, auto start, torrent file association, custom background/theme, six UI languages, single-instance command line handling.

<!-- ABOUT -->
## About

| Platform | Support |
|:--|:--|
| 🪟 **Windows** | Full support (bundled BT natives) |
| 🐧 **Linux** | Full support (`x86_64` / `arm64` natives) |
| 🍎 **macOS** | Core features work; BT requires supplying jlibtorrent natives yourself |

> [!WARNING]
> `lib/` currently ships jlibtorrent natives for Windows and Linux only, so BT is unavailable on macOS. See [Build & packaging][build-doc] (Chinese).

| Item | Value |
|:--|:--|
| Version | `0.4.5.2` |
| Plugin API version | `1.1.2` |
| Runtime | `JDK 21+` |
| Plugin repository | [wmp666/Speed_Bump_Plugin](https://github.com/wmp666/Speed_Bump_Plugin) |
| License | [Apache License 2.0](LICENSE) |

<!-- QUICK START -->
## Quick start

1. Grab an installer from [Releases][release-url] (`Speed_Bump_Setup_*.exe` for Windows, `Speed_Bump_Setup_*.deb` for Linux)
2. Click "Create task" and paste your links, one per line — or simply **drop a torrent / image file into the text box**
3. Task cards support pause, resume, open file, open containing folder, and optional deletion of local files on close

Building from source: see [Build & packaging][build-doc] (Chinese).

<!-- LICENSE -->
## License

Distributed under the [Apache License 2.0](LICENSE).

This tool is intended for **personal learning, technical research and academic exchange** only. See the full disclaimer in [helpDocs](helpDocs/鸣谢与声明.md#声明) (Chinese).

<!-- MARKDOWN LINKS & IMAGES -->
[docs-url]: helpDocs/README.md
[build-doc]: helpDocs/构建与打包.md
[release-shield]: https://img.shields.io/github/v/release/wmp666/Speed_Bump?style=for-the-badge
[release-url]: https://github.com/wmp666/Speed_Bump/releases/latest
[license-shield]: https://img.shields.io/github/license/wmp666/Speed_Bump?style=for-the-badge
[license-url]: LICENSE
[stars-shield]: https://img.shields.io/github/stars/wmp666/Speed_Bump.svg?style=for-the-badge
[stars-url]: https://github.com/wmp666/Speed_Bump/stargazers
[issues-shield]: https://img.shields.io/github/issues/wmp666/Speed_Bump.svg?style=for-the-badge
[issues-url]: https://github.com/wmp666/Speed_Bump/issues
[downloads-shield]: https://img.shields.io/github/downloads/wmp666/Speed_Bump/total?style=for-the-badge
[java-shield]: https://img.shields.io/badge/Java-21%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white
[platform-shield]: https://img.shields.io/badge/platform-Windows%20%7C%20Linux-blue?style=for-the-badge
