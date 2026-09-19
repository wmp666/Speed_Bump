param(
    [string]$SourceRoot = "src",
    [string]$BasePath = "com/wmp/speed_bump/common/background/resource/icon",
    [string]$OutputJar = "lib/icons.jar"
)

$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = (Get-Location).Path }
$projectRoot = [System.IO.Path]::GetFullPath($projectRoot)

if (-not (Test-Path (Join-Path $projectRoot (Join-Path $SourceRoot $BasePath)))) {
    throw "icon source dir not found: $SourceRoot/$BasePath"
}

$jarExe = $null

# 1. JAVA_HOME
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin/jar.exe"
    if (Test-Path $candidate) { $jarExe = $candidate }
}

# 2. PATH 中 java 的同目录
if (-not $jarExe) {
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) {
        $candidate = Join-Path (Split-Path $java.Source) "jar.exe"
        if (Test-Path $candidate) { $jarExe = $candidate }
    }
}

# 3. PATH 中的 jar
if (-not $jarExe) {
    $jar = Get-Command jar -ErrorAction SilentlyContinue
    if ($jar) { $jarExe = $jar.Source }
}

# 4. 常见 JDK 安装目录
if (-not $jarExe) {
    $searchRoots = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        (Join-Path $env:LOCALAPPDATA "Programs\Eclipse Adoptium")
    )
    $found = Get-ChildItem $searchRoots -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName "bin/jar.exe" } |
        Where-Object { Test-Path $_ } |
        Select-Object -First 1
    if ($found) { $jarExe = $found }
}

if (-not $jarExe) {
    throw "jar not found; set JAVA_HOME or add JDK bin to PATH"
}

Write-Host "using jar: $jarExe"

if ([System.IO.Path]::IsPathRooted($OutputJar)) {
    $target = [System.IO.Path]::GetFullPath($OutputJar)
} else {
    $target = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $OutputJar))
}
$targetDir = Split-Path $target
if (-not [System.IO.Directory]::Exists($targetDir)) {
    [System.IO.Directory]::CreateDirectory($targetDir) | Out-Null
}

# jar 的 -C 是切换工作目录，这里切到 src 根，
# 条目名必须是 com/wmp/... 的包路径，classpath 的 getResource 才能命中。
$srcDir = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $SourceRoot))
$tmpJar = Join-Path $projectRoot "icons.tmp.jar"
Remove-Item -LiteralPath $tmpJar -ErrorAction SilentlyContinue

& $jarExe cf $tmpJar -C $srcDir $BasePath
if ($LASTEXITCODE -ne 0) {
    Remove-Item -LiteralPath $tmpJar -ErrorAction SilentlyContinue
    throw "pack failed"
}

# 复制到目标：直接让 jar 写目标文件时，杀软扫描等会导致“文件被占用”
$copied = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {
    try {
        [System.IO.File]::Copy($tmpJar, $target, $true)
        $copied = $true
        break
    } catch {
        Write-Host "copy attempt $attempt failed: $($_.Exception.Message)"
        Start-Sleep -Milliseconds 1000
    }
}
Remove-Item -LiteralPath $tmpJar -ErrorAction SilentlyContinue

if (-not $copied) { throw "cannot write $target" }

$probe = "$BasePath/light/12-misc/circle.png"
if (-not (& $jarExe tf $target | Where-Object { $_ -eq $probe })) {
    throw "entry path wrong, expected $probe"
}

$size = "{0:N2} MB" -f ((Get-Item $target).Length / 1MB)
$count = (& $jarExe tf $target | Measure-Object).Count
Write-Host "built  $target ($count entries, $size)"
Write-Host "lib/icons.jar 已更新，IDEA 中重新 Build Artifacts 即可生效。" -ForegroundColor Green
