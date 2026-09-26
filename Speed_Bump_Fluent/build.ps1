# =====================================================================
#  Speed_Bump_Fluent 构建脚本
#
#  用法：
#    powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1            # 编译到 out/classes
#    powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Clean     # 清理后重新编译
#    powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -Jar       # 另出可执行 jar
#
#  注意：本脚本必须保存为「UTF-8 with BOM」。Windows PowerShell 5.1 对无 BOM 的 .ps1
#  按 ANSI 解码，中文注释会变成乱码并直接引发语法错误。
#
#  说明：本项目刻意不使用 Maven / Gradle，与旧项目（DownLoader）保持一致——
#  依赖以 jar 形式放在 lib/，构建只依赖 JDK 自带的 javac / jar。
# =====================================================================

[CmdletBinding()]
param(
    [switch]$Clean,
    [switch]$Jar
)

$ErrorActionPreference = 'Stop'

$root    = $PSScriptRoot
$srcDir  = Join-Path $root 'src'
$outRoot = Join-Path $root 'out'
$outDir  = Join-Path $outRoot 'classes'
$libDir  = Join-Path $root 'lib'
$jarFile = Join-Path $outRoot 'Speed_Bump_Fluent.jar'
$mainClass = 'com.wmp.speedbump.fluent.FluentApp'

if ($Clean -and (Test-Path $outRoot)) {
    Remove-Item $outRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# ---- 依赖 ----
$jars = @(Get-ChildItem -Path $libDir -Filter *.jar -File -ErrorAction SilentlyContinue)
$classpath = ($jars | ForEach-Object { $_.FullName }) -join [IO.Path]::PathSeparator
if ($jars.Count -eq 0) {
    Write-Warning "lib/ 下没有 jar，编译可能因缺少 FlatLaf 而失败"
}

# ---- 源文件 ----
$sources = @(Get-ChildItem -Path $srcDir -Recurse -Filter *.java -File)
if ($sources.Count -eq 0) {
    throw "src/ 下没有找到任何 .java 文件"
}
Write-Host "[1/3] 编译 $($sources.Count) 个源文件 -> $outDir" -ForegroundColor Cyan
$sourcePaths = $sources | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -Xlint:-options -d $outDir -cp $classpath @sourcePaths
if ($LASTEXITCODE -ne 0) {
    throw "javac 编译失败（exit $LASTEXITCODE）"
}

# ---- 资源（如果有） ----
$resourceDir = Join-Path $root 'resource'
if (Test-Path $resourceDir) {
    Write-Host "[2/3] 复制资源 -> $outDir" -ForegroundColor Cyan
    Copy-Item -Path (Join-Path $resourceDir '*') -Destination $outDir -Recurse -Force
} else {
    Write-Host "[2/3] 无 resource/ 目录，跳过" -ForegroundColor DarkGray
}

# ---- 打包 ----
if ($Jar) {
    Write-Host "[3/3] 打包 -> $jarFile" -ForegroundColor Cyan
    # 把依赖复制到 out/lib，并在 Manifest 里用相对 Class-Path 引用，
    # 这样 jar 与 out/lib 一起分发即可直接 java -jar 运行
    $outLib = Join-Path $outRoot 'lib'
    New-Item -ItemType Directory -Force -Path $outLib | Out-Null
    $cpEntries = @()
    foreach ($j in $jars) {
        Copy-Item $j.FullName $outLib -Force
        $cpEntries += "lib/$($j.Name)"
    }
    $manifest = Join-Path $outRoot 'MANIFEST.MF'
    $lines = @(
        'Manifest-Version: 1.0',
        "Main-Class: $mainClass",
        "Class-Path: $($cpEntries -join ' ')",
        'Enable-Native-Access: ALL-UNNAMED',
        ''
    )
    Set-Content -Path $manifest -Value $lines -Encoding ASCII
    & jar --create --file $jarFile --manifest $manifest -C $outDir .
    if ($LASTEXITCODE -ne 0) {
        throw "jar 打包失败（exit $LASTEXITCODE）"
    }
    Write-Host "完成：$jarFile" -ForegroundColor Green
} else {
    Write-Host "[3/3] 未指定 -Jar，跳过打包" -ForegroundColor DarkGray
}

Write-Host "编译成功：$outDir" -ForegroundColor Green
