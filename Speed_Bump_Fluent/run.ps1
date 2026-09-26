# =====================================================================
#  Speed_Bump_Fluent 运行脚本
#
#  用法：
#    powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1
#    powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -AppArgs @('--theme=dark','--static')
#
#  必须先执行 build.ps1。装了 PowerShell 7 的话也可以直接 pwsh -File run.ps1。
#  注意 -AppArgs 要传数组时请用 -Command 调用或直接调 java，
#  经 powershell -File 传参时多个取值会绑定失败（这一点踩过坑）。
#
#  关于 --enable-native-access：
#    背景材质走 JDK FFM（java.lang.foreign）直接调 dwmapi/user32，
#    JDK 24 起不加该参数会打印「restricted method」警告（功能仍然正常）。
# =====================================================================

[CmdletBinding()]
param(
    [string[]]$AppArgs = @(),
    [switch]$Jar
)

$ErrorActionPreference = 'Stop'

$root      = $PSScriptRoot
$outRoot   = Join-Path $root 'out'
$classes   = Join-Path $outRoot 'classes'
$libDir    = Join-Path $root 'lib'
$jarFile   = Join-Path $outRoot 'Speed_Bump_Fluent.jar'
$mainClass = 'com.wmp.speedbump.fluent.FluentApp'

$jvmArgs = @(
    '--enable-native-access=ALL-UNNAMED',
    '-Dfile.encoding=UTF-8',
    '-Dsun.java2d.uiScale.enabled=true'
)

if ($Jar) {
    if (-not (Test-Path $jarFile)) {
        throw "找不到 $jarFile，请先执行：pwsh -File build.ps1 -Jar"
    }
    Write-Host "运行 jar: $jarFile" -ForegroundColor Cyan
    & java @jvmArgs -jar $jarFile @AppArgs
    exit $LASTEXITCODE
}

if (-not (Test-Path $classes)) {
    throw "找不到 $classes，请先执行：pwsh -File build.ps1"
}

$jars = @(Get-ChildItem -Path $libDir -Filter *.jar -File -ErrorAction SilentlyContinue)
$classpath = @($classes) + ($jars | ForEach-Object { $_.FullName })
$classpath = $classpath -join [IO.Path]::PathSeparator

Write-Host "运行 $mainClass" -ForegroundColor Cyan
& java @jvmArgs -cp $classpath $mainClass @AppArgs
exit $LASTEXITCODE
