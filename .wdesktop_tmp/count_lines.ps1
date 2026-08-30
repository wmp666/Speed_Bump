$ErrorActionPreference = 'Stop'

function Get-EffectiveLines([string]$path) {
    $lines = [System.IO.File]::ReadAllLines($path)
    $total = $lines.Count
    $inBlock = $false
    $effective = 0
    foreach ($line in $lines) {
        $i = 0
        $len = $line.Length
        $code = $false
        while ($i -lt $len) {
            $c = $line[$i]
            if ($inBlock) {
                if ($c -eq '*' -and ($i + 1) -lt $len -and $line[$i + 1] -eq '/') { $inBlock = $false; $i += 2 }
                else { $i++ }
            } else {
                if ($c -eq '/' -and ($i + 1) -lt $len) {
                    $n = $line[$i + 1]
                    if ($n -eq '/') { break }
                    elseif ($n -eq '*') { $inBlock = $true; $i += 2 }
                    else { $code = $true; $i++ }
                }
                elseif ($c -eq '"' -or $c -eq "'") {
                    $quote = $c
                    $code = $true
                    $i++
                    while ($i -lt $len) {
                        if ($line[$i] -eq '\' -and ($i + 1) -lt $len) { $i += 2 }
                        elseif ($line[$i] -eq $quote) { $i++; break }
                        else { $i++ }
                    }
                }
                else {
                    if ($c -ne ' ' -and $c -ne "`t" -and $c -ne "`r") { $code = $true }
                    $i++
                }
            }
        }
        if ($code) { $effective++ }
    }
    return @{ Total = $total; Effective = $effective }
}

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$srcDir = Join-Path $root 'src'
$tmpDir = Join-Path $root '.wdesktop_tmp'
$files = @()
$files += Get-ChildItem -Path $srcDir -Filter '*.java' -Recurse -File
$files += Get-ChildItem -Path $tmpDir -Filter '*.java' -Recurse -File

$results = @()
foreach ($f in $files) {
    $rel = $f.FullName.Substring($root.Length + 1)
    $r = Get-EffectiveLines $f.FullName
    if ($rel -like '.wdesktop_tmp*') { $cat = 'temp(.wdesktop_tmp)' }
    elseif ($rel -match '\\test\\') { $cat = 'test(pkg)' }
    else { $cat = 'main(src)' }
    $results += [pscustomobject]@{ Path = $rel; Total = $r.Total; Effective = $r.Effective; Cat = $cat }
}

Write-Output '=== BY CATEGORY ==='
$results | Group-Object Cat | ForEach-Object {
    $tf = ($_.Group | Measure-Object Total -Sum).Sum
    $ef = ($_.Group | Measure-Object Effective -Sum).Sum
    [pscustomobject]@{
        Category = $_.Name
        Files    = $_.Count
        Total    = $tf
        Effective= $ef
        Comment  = $tf - $ef
    }
} | Format-Table -AutoSize

Write-Output '=== TOTAL ==='
$tAll = ($results | Measure-Object Total -Sum).Sum
$eAll = ($results | Measure-Object Effective -Sum).Sum
[pscustomobject]@{
    Files     = $results.Count
    Total     = $tAll
    Effective = $eAll
    Comment   = $tAll - $eAll
    Ratio     = ('{0:P1}' -f ($eAll / $tAll))
} | Format-List

Write-Output '=== TOP 15 BY EFFECTIVE ==='
$results | Sort-Object Effective -Descending | Select-Object -First 15 Path, Effective, Total | Format-Table -AutoSize
