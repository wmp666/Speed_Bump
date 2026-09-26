# =====================================================================
#  减速带 · 界面自检（离屏渲染 + 断言 + 截图）
#
#  用途：
#    在不启动主窗口的前提下，构建一个组件画廊、离屏渲染成 PNG、
#    并对「开关外观 / 传统复选框回退 / 表格 Boolean 渲染器 / 进度条 /
#    滚动条 / 揭示高亮 / 主题切换后 UI 默认值存活」逐项断言。
#
#  用法：
#    powershell -NoProfile -ExecutionPolicy Bypass -File .\run-ui-selftest.ps1
#    powershell -NoProfile -ExecutionPolicy Bypass -File .\run-ui-selftest.ps1 -Dark
#    powershell -NoProfile -ExecutionPolicy Bypass -File .\run-ui-selftest.ps1 -Theme "Darcula"
#
#  产物：out/ui-selftest/
#    gallery-light.png / gallery-dark.png   界面截图（人工复核用）
#    component-tree-*.txt                   组件树与每个组件的实际尺寸
#    report.txt                             UTF-8 报告（含全部断言结果）
#
#  退出码：0 = 全部通过；1 = 有问题（详见 report.txt）。
#
#  说明：
#    1) 本项目没有 Ant / Maven / Gradle，日常靠 IntelliJ 编译。
#       本脚本用根目录的 out-cp.txt（依赖 classpath）直接调 javac，
#       这样在没有 IDE 的环境里也能验证。
#    2) 编译产物放在 out/ui-selftest-classes（out/ 已被 .gitignore 忽略），
#       不会污染仓库里的 .compilecheck。
#    3) -Duser.home 指向 out/ui-selftest-home，把配置与日志隔离在项目内，
#       既不写用户真实的 ~/.speed-bump，也避免受限环境下写入家目录被拒。
# =====================================================================

[CmdletBinding()]
param(
    [switch]$Dark,
    [string]$Theme,
    [switch]$SkipCompile
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$outRoot = Join-Path $root 'out'
$classesDir = Join-Path $outRoot 'ui-selftest-classes'
$selftestOut = Join-Path $outRoot 'ui-selftest'
$isolatedHome = Join-Path $outRoot 'ui-selftest-home'
$mainClass = 'com.wmp.downloader.tools.devtools.UiSelfTest'

function Write-Step($text) { Write-Host "[ui-selftest] $text" -ForegroundColor Cyan }

# ---- 1) 依赖 classpath ----
$cpFile = Join-Path $root 'out-cp.txt'
if (Test-Path $cpFile) {
    # 必须 Trim：文件末尾的换行会把最后一个条目污染成非法路径
    # （表现为「明明 jar 存在却报找不到包」，很难查）
    $classpath = (Get-Content $cpFile -Raw).Trim()
    Write-Step "使用 out-cp.txt 作为依赖 classpath"
} else {
    Write-Step "out-cp.txt 不存在，从 .idea/libraries 与 lib/ 现算一份"
    $m2 = Join-Path $env:USERPROFILE '.m2\repository'
    $jars = New-Object System.Collections.Generic.List[string]
    Get-ChildItem (Join-Path $root '.idea\libraries\*.xml') -ErrorAction SilentlyContinue | ForEach-Object {
        [xml]$x = Get-Content $_.FullName
        $x.SelectNodes('//root/@url') | ForEach-Object {
            $u = $_.Value -replace '\$MAVEN_REPOSITORY\$', $m2 -replace '\$PROJECT_DIR\$', $root
            if ($u -match '^jar://(.*)!/$') {
                $p = $matches[1]
                if ($p -notmatch 'sources|javadoc' -and (Test-Path $p)) { $jars.Add($p) }
            }
        }
    }
    Get-ChildItem (Join-Path $root 'lib\*.jar') -ErrorAction SilentlyContinue | ForEach-Object { $jars.Add($_.FullName) }
    # IntelliJ 会自带 org.jetbrains.annotations，命令行编译需要显式加上
    $annotation = Get-ChildItem "$m2\org\jetbrains\annotations" -Recurse -Filter 'annotations-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources|javadoc' } | Sort-Object Name -Descending | Select-Object -First 1
    if ($annotation) { $jars.Add($annotation.FullName) }
    $classpath = (($jars | Select-Object -Unique) -join ';')
}

# ---- 2) 编译 ----
if (-not $SkipCompile) {
    Write-Step "编译到 $classesDir"
    if (Test-Path $classesDir) { Remove-Item $classesDir -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
    $sources = @(Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java -File)
    if ($sources.Count -eq 0) { throw "src/ 下没有 .java 文件" }
    $sourcePaths = $sources | ForEach-Object { $_.FullName }
    & javac -encoding UTF-8 -nowarn -d $classesDir -cp $classpath @sourcePaths
    if ($LASTEXITCODE -ne 0) { throw "javac 编译失败（exit $LASTEXITCODE）" }
    Write-Step "编译完成：$($sources.Count) 个源文件"
} elseif (-not (Test-Path $classesDir)) {
    throw "指定了 -SkipCompile 但 $classesDir 不存在"
}

# ---- 3) 运行自检 ----
# src 也放进 classpath：laug 语言包、图标等资源都在 src 下，
# javac 只编译 .java，不会把资源复制到输出目录
$runCp = "$classesDir;$($root)\src;$classpath"
New-Item -ItemType Directory -Force -Path $selftestOut, $isolatedHome | Out-Null

# 允许传中文主题名（PowerShell 5.1 下参数已是 Unicode，这里只做空格保护）
$appArgs = @()
if ($Dark) { $appArgs += '--dark' }
if ($Theme) { $appArgs += "--theme=$Theme" }

Write-Step "运行 $mainClass（配置目录隔离在 $isolatedHome）"
# 注意：原生命令往 stderr 写东西时（本项目 log4j 会往 stderr 打日志），
# 在 $ErrorActionPreference='Stop' 且 stderr 被合并的情况下，那些错误记录会变成
# 终止性错误，脚本于是以失败状态收场——即使自检本身是通过的。这里临时放宽。
$previousEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    & java "-Duser.home=$isolatedHome" '-Dfile.encoding=UTF-8' '-Dspeedbump.trace.startup=false' `
        -cp $runCp $mainClass @appArgs
    $exit = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousEap
}

# ---- 4) 汇总 ----
Write-Host ""
Write-Step "退出码：$exit"
if (Test-Path (Join-Path $selftestOut 'report.txt')) {
    Write-Host "-------- 报告 --------" -ForegroundColor DarkGray
    Get-Content (Join-Path $selftestOut 'report.txt') -Encoding UTF8 | ForEach-Object { Write-Host $_ }
    Write-Host "----------------------" -ForegroundColor DarkGray
}
exit $exit
