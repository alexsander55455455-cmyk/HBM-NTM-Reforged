<#
.SYNOPSIS
  Fast startup smoke: auto-join test world and verify mod loads (no SendKeys).

.DESCRIPTION
  Tier 1 (seconds):  .\gradlew.bat verifyCreativeTabSort
  Tier 2 (minutes):  .\tools\smoke_quick.ps1

  Sets JoinWorldOnLaunch=test, launches PrismLauncher, watches latest.log for FML + world load.

.PARAMETER SkipDeploy
  Do not copy the release JAR into the instance mods folder.

.PARAMETER KeepRunning
  Leave javaw running after PASS/FAIL (faster re-runs if MC stays open).

.PARAMETER InstanceName
  PrismLauncher instance name. Default: "1.12.2 REFORGE"
#>
param(
    [switch]$SkipDeploy,
    [switch]$KeepRunning,
    [string]$InstanceName = '1.12.2 REFORGE'
)

$ErrorActionPreference = 'Continue'

$proj = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $proj 'build\libs\HBM-NTM-Reforged-0.9.0-alpha.jar'
$prism = Join-Path $env:LOCALAPPDATA 'Programs\PrismLauncher\prismlauncher.exe'
$instRoot = Join-Path $env:APPDATA "PrismLauncher\instances\$InstanceName"
$mods = Join-Path $instRoot 'minecraft\mods'
$log = Join-Path $instRoot 'minecraft\logs\latest.log'
$saves = Join-Path $instRoot 'minecraft\saves'
$instanceCfg = Join-Path $instRoot 'instance.cfg'
$marker = "SMOKE_QUICK_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
$scratch = if ($env:GOAL_SCRATCH) { $env:GOAL_SCRATCH } else { 'C:\Temp\grok-goal-a85ccd828ffa\implementer' }
New-Item -ItemType Directory -Force -Path $scratch | Out-Null
$passPattern = 'Done \(|Starting integrated minecraft server'
$requiredPatterns = @(
    'Forge Mod Loader has successfully loaded'
)
$backupCfg = Join-Path $env:TEMP "hbm-instance.cfg.bak-$marker"

if (-not (Test-Path $jar)) {
    Write-Error "Release JAR missing. Build first: .\gradlew.bat verifyCreativeTabSort build -x test --no-daemon"
    exit 2
}
if (-not (Test-Path $prism)) {
    Write-Error "PrismLauncher not found: $prism"
    exit 2
}
if (-not (Test-Path $instanceCfg)) {
    Write-Error "Instance cfg missing: $instanceCfg"
    exit 2
}

function Wait-ProcessStarted([string]$name, [int]$sec) {
    $deadline = (Get-Date).AddSeconds($sec)
    while ((Get-Date) -lt $deadline) {
        if (Get-Process $name -ErrorAction SilentlyContinue) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Wait-LogPattern([string]$pattern, [int]$sec) {
    $deadline = (Get-Date).AddSeconds($sec)
    while ((Get-Date) -lt $deadline) {
        if ((Test-Path $log) -and (Select-String -Path $log -Pattern $pattern -Quiet)) { return $true }
        if (-not (Get-Process javaw -ErrorAction SilentlyContinue) -and (Test-Path $log)) { return $false }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Set-InstanceSmokeFlags {
    param([string]$joinWorld, [string]$jvmArgs)
    $cfg = Get-Content $instanceCfg -Raw
    if ($cfg -notmatch '\[General\]') {
        $cfg = "[General]`n" + $cfg
    }
    if ($cfg -match 'JoinWorldOnLaunch=') {
        $cfg = $cfg -replace 'JoinWorldOnLaunch=.*', "JoinWorldOnLaunch=$joinWorld"
    } else {
        $cfg = $cfg -replace '\[General\]', "[General]`nJoinWorldOnLaunch=$joinWorld"
    }
    if ($cfg -match 'OverrideJavaArgs=') {
        $cfg = $cfg -replace 'OverrideJavaArgs=.*', 'OverrideJavaArgs=true'
    } else {
        $cfg = $cfg -replace 'JoinWorldOnLaunch=', "OverrideJavaArgs=true`nJoinWorldOnLaunch="
    }
    if ($cfg -match 'JvmArgs=') {
        $cfg = $cfg -replace 'JvmArgs=.*', "JvmArgs=$jvmArgs"
    } else {
        $cfg = $cfg -replace 'OverrideJavaArgs=true', "OverrideJavaArgs=true`nJvmArgs=$jvmArgs"
    }
    Set-Content $instanceCfg $cfg -NoNewline
}

Write-Host "[$marker] deploy=$([bool](-not $SkipDeploy)) keep=$KeepRunning"

if (-not $SkipDeploy) {
    Copy-Item $jar (Join-Path $mods 'HBM-NTM-Reforged-0.9.0-alpha.jar') -Force
}

$dd = Join-Path $saves 'dd'
$test = Join-Path $saves 'test'
if ((Test-Path $dd) -and -not (Test-Path (Join-Path $test 'level.dat'))) {
    Write-Host "[$marker] seeding saves\test from saves\dd"
    robocopy $dd $test /MIR /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
}

Copy-Item $instanceCfg $backupCfg -Force
Set-InstanceSmokeFlags -joinWorld 'test' -jvmArgs ''

if (-not $KeepRunning) {
    Stop-Process -Name javaw -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
}
if (Test-Path $log) { Remove-Item $log -Force }

$launchArgs = "-l `"$InstanceName`" -w test"
Write-Host "[$marker] launching: $prism $launchArgs"
Start-Process -FilePath $prism -ArgumentList $launchArgs -WindowStyle Normal | Out-Null

$javaw = Wait-ProcessStarted 'javaw' 120
$fml = $false
$world = $false
$pass = $false

if ($javaw) {
    $fml = Wait-LogPattern 'Forge Mod Loader has successfully loaded' 600
}
if ($fml) {
    $world = Wait-LogPattern 'Starting integrated minecraft server|Loading dimension 0|Done \(' 300
}
if ($world) {
    $pass = Wait-LogPattern $passPattern 240
}

$crashHits = @()
if (Test-Path $log) {
    $crashHits = Select-String -Path $log -Pattern 'selectedTabIndex|NoSuchFieldException|ReflectionHelper.*selectedTab|Stopping server' -ErrorAction SilentlyContinue
}

Copy-Item $backupCfg $instanceCfg -Force
Remove-Item $backupCfg -Force -ErrorAction SilentlyContinue

if (-not $KeepRunning) {
    Stop-Process -Name javaw -Force -ErrorAction SilentlyContinue
}

$ji = Get-Item $jar
$result = [ordered]@{
    marker          = $marker
    jar_mtime       = $ji.LastWriteTime.ToString('o')
    javaw_started   = $javaw
    fml_loaded      = $fml
    world_loaded    = $world
    smoke_pass      = $pass
    crash_signature = ($crashHits.Count -gt 0)
}
if (Test-Path $log) {
    $li = Get-Item $log
    $result.log_path = $li.FullName
    $result.log_mtime = $li.LastWriteTime.ToString('o')
    $result.log_bytes = $li.Length
}

$report = Join-Path $scratch 'creative-smoke-log.txt'
$out = @("marker=$marker")
$result.GetEnumerator() | ForEach-Object { $out += "$($_.Key)=$($_.Value)" }
if (Test-Path $jar) {
    $out += "jar_deployed_mtime=$((Get-Item $jar).LastWriteTime.ToString('o'))"
}
foreach ($rp in $requiredPatterns) {
    $hits = @()
    if (Test-Path $log) { $hits = Select-String -Path $log -Pattern $rp }
    $out += "--- required=$rp count=$($hits.Count) ---"
    $hits | Select-Object -First 3 | ForEach-Object { $out += "L$($_.LineNumber): $($_.Line)" }
}
$patterns = @('selectedTabIndex','ReflectionHelper','NoSuchFieldException','Stopping server','search tab HBM sort applied')
if (Test-Path $log) {
    foreach ($p in $patterns) {
        $hits = Select-String -Path $log -Pattern $p
        $out += "--- $p count=$($hits.Count) ---"
        $hits | Select-Object -First 8 | ForEach-Object { $out += "L$($_.LineNumber): $($_.Line)" }
    }
    $out += '--- tail 150 ---'
    $out += Get-Content $log -Tail 150
}
$out | Set-Content $report -Encoding UTF8
Set-Content (Join-Path $scratch 'jar-timestamp.txt') "jar_path=$jar`njar_mtime=$((Get-Item $jar).LastWriteTime.ToString('o'))`njar_bytes=$((Get-Item $jar).Length)`nsmoke_marker=$marker" -Encoding UTF8

$requiredOk = $true
foreach ($rp in $requiredPatterns) {
    if (-not (Test-Path $log) -or -not (Select-String -Path $log -Pattern $rp -Quiet)) {
        $requiredOk = $false
    }
}
$ok = $pass -and $requiredOk -and ($crashHits.Count -eq 0)
Write-Host "[$marker] PASS=$pass required=$requiredOk crash_hits=$($crashHits.Count) report=$report"
if ($ok) { exit 0 }
exit 1