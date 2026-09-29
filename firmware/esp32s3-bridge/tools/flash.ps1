<#
.SYNOPSIS
    PocketHID ESP32-S3 Flash Script — Wave 9R.1
    Flashes the SHA-256-verified Wave 8 firmware to an ESP32-S3 board.

.DESCRIPTION
    Safe flash script for PocketHID ESP32-S3 bridge firmware.

    Firmware artifact (Wave 8 cross-compiled, ESP-IDF v5.3.1):
      merged-binary.bin     SHA-256: D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD
      pockethid-esp32s3-bridge.bin
                            SHA-256: 5D06784D36942B760AB0318271E51DA82B8DED8A742FDC1790AC4DFEB72A05E3
      bootloader.bin        SHA-256: D8016C8FD0D219CA09708B5D38E8DA64DE6A24FEBE6BE63FC83EFCD54F194E9D
      partition-table.bin   SHA-256: 7F00B6C042A89B15B0CAC534F82ED988CAF29278FF5700B0C511EB1B5BB7C820

    Git commit   : 160fddfa6763989cebe0aca67efe2d2e1dbdc65a
    ESP-IDF ver  : v5.3.1
    Compiler     : xtensa-esp32s3-elf-gcc (esp-13.2.0_20240530)
    Target chip  : esp32s3
    Flash mode   : DIO
    Flash freq   : 80 MHz
    Flash size   : 2 MB

.PARAMETER Port
    Serial port of the ESP32-S3 board (e.g., COM3, COM4).
    REQUIRED — no default is provided.
    COM6 and COM7 are Bluetooth SPP links and are EXPLICITLY REJECTED.

    To identify the correct port:
      Get-PnpDevice -Class Ports | Select-Object Name, InstanceId
      Get-PnpDevice | Where-Object {$_.InstanceId -match 'VID_303A|VID_10C4|VID_1A86|VID_0403'} | Select-Object Name, InstanceId

.PARAMETER Mode
    'merged'   : Flash merged-binary.bin at 0x0  [PREFERRED]
    'separate' : Flash bootloader + partition-table + app separately

.PARAMETER Baud
    Flash baud rate. Default: 921600.
    Lower to 460800 or 230400 if "Failed to connect" errors occur.

.PARAMETER Erase
    Switch. Performs chip_erase before flashing.
    DESTROYS all NVS data and BLE pairing info.
    Default: NOT set. Must be explicitly passed as -Erase.

.EXAMPLE
    # Step 1: identify the board's COM port
    Get-PnpDevice -Class Ports | Select-Object Name, InstanceId

    # Step 2: flash (merged binary — preferred)
    .\flash.ps1 -Port COM3

    # Step 3: lower baud on connection errors
    .\flash.ps1 -Port COM3 -Baud 460800

    # Step 4: erase only when required (destroys NVS)
    .\flash.ps1 -Port COM3 -Erase

.NOTES
    Wave 9R.1 — PATH B Closeout
    NEVER run with COM6 or COM7 (Bluetooth SPP links — not ESP32).
    Record ALL console output verbatim into FLASH-MANIFEST.md evidence section.
    Do NOT modify Port between board identification and this flash command.
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$Port,

    [ValidateSet('merged', 'separate')]
    [string]$Mode = 'merged',

    [int]$Baud = 921600,

    [switch]$Erase
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ─── Paths (resolved relative to script location, not CWD) ─────────────────────
$ScriptDir   = $PSScriptRoot
$BuildDir    = Join-Path $ScriptDir ".." "build"
$BuildDir    = [System.IO.Path]::GetFullPath($BuildDir)   # no Resolve-Path — works even if run from other dirs

$MergedBin     = Join-Path $BuildDir "merged-binary.bin"
$AppBin        = Join-Path $BuildDir "pockethid-esp32s3-bridge.bin"
$BootloaderBin = Join-Path $BuildDir "bootloader" "bootloader.bin"
$PartTableBin  = Join-Path $BuildDir "partition_table" "partition-table.bin"

# ─── Known-good SHA-256 hashes — Wave 8 artifacts, ESP-IDF v5.3.1 ────────────
$KnownHashes = @{
    $MergedBin     = "D75D123954ECDCE4EEB9B1CD7BF0336AC8D0EACFD546ED8C5E9C58E4747944BD"
    $AppBin        = "5D06784D36942B760AB0318271E51DA82B8DED8A742FDC1790AC4DFEB72A05E3"
    $BootloaderBin = "D8016C8FD0D219CA09708B5D38E8DA64DE6A24FEBE6BE63FC83EFCD54F194E9D"
    $PartTableBin  = "7F00B6C042A89B15B0CAC534F82ED988CAF29278FF5700B0C511EB1B5BB7C820"
}

# ─── SAFETY: Blocked BT SPP ports ──────────────────────────────────────────────
# COM6 and COM7 are "Standard Serial over Bluetooth link" on the development machine.
# Flashing to these ports would attempt serial communication with a Bluetooth device,
# not an ESP32-S3. This guard prevents that mistake.
$BlockedPorts = @('COM6', 'COM7')
if ($BlockedPorts -contains $Port.ToUpper()) {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Red
    Write-Host "  ERROR: $Port is a Bluetooth SPP link, NOT an ESP32-S3." -ForegroundColor Red
    Write-Host "  COM6 and COM7 are blocked on this machine."             -ForegroundColor Red
    Write-Host "  Connect an ESP32-S3 board and identify its COM port:"   -ForegroundColor Red
    Write-Host "    Get-PnpDevice -Class Ports | Select-Object Name, InstanceId" -ForegroundColor Yellow
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Red
    exit 1
}

# ─── Helper: SHA-256 verify ────────────────────────────────────────────────────
function Assert-SHA256 {
    param([string]$FilePath, [string]$ExpectedHash)
    if (-not (Test-Path $FilePath)) {
        Write-Host "  MISSING: $FilePath" -ForegroundColor Red
        Write-Error "Firmware artifact not found: $FilePath"
    }
    $actual = (Get-FileHash $FilePath -Algorithm SHA256).Hash
    Write-Host "  File   : $(Split-Path $FilePath -Leaf)"
    Write-Host "  Expected SHA-256: $ExpectedHash"
    Write-Host "  Actual   SHA-256: $actual"
    if ($actual -ne $ExpectedHash) {
        Write-Host "  SHA-256: MISMATCH" -ForegroundColor Red
        Write-Error "SHA-256 MISMATCH for $(Split-Path $FilePath -Leaf). Do NOT flash — verify artifact integrity."
    }
    Write-Host "  SHA-256: OK" -ForegroundColor Green
    Write-Host ""
}

# ─── Banner ────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "╔═══════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     PocketHID ESP32-S3 Flash Script — Wave 9R.1           ║" -ForegroundColor Cyan
Write-Host "╠═══════════════════════════════════════════════════════════╣" -ForegroundColor Cyan
Write-Host "║  Git commit  : 160fddfa6763989cebe0aca67efe2d2e1dbdc65a   ║" -ForegroundColor Cyan
Write-Host "║  ESP-IDF     : v5.3.1                                     ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Target port  : $Port"     -ForegroundColor Yellow
Write-Host "  Mode         : $Mode"
Write-Host "  Baud         : $Baud"
Write-Host "  Erase first  : $($Erase.IsPresent)"
Write-Host "  Build dir    : $BuildDir"
Write-Host ""

# ─── esptool check ─────────────────────────────────────────────────────────────
Write-Host "── Checking esptool.py ──" -ForegroundColor Yellow
try {
    $esptoolOut = & esptool.py version 2>&1
    Write-Host "  esptool.py: $esptoolOut" -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "  esptool.py not found." -ForegroundColor Red
    Write-Host "  Install: pip install esptool"
    Write-Host "  Or activate ESP-IDF: idf_cmd_init.bat (Windows)"
    Write-Error "esptool.py not available."
}
Write-Host ""

# ─── Verify port is accessible ─────────────────────────────────────────────────
Write-Host "── Verifying serial port $Port ──" -ForegroundColor Yellow
$portInfo = Get-PnpDevice -Class Ports -ErrorAction SilentlyContinue | Where-Object { $_.Name -match [regex]::Escape($Port) }
if ($portInfo) {
    Write-Host "  Port found : $($portInfo.Name)" -ForegroundColor Green
    Write-Host "  InstanceId : $($portInfo.InstanceId)"
} else {
    Write-Host "  WARNING: $Port not found in PnP device list." -ForegroundColor Yellow
    Write-Host "  This may be normal if the driver uses a generic name."
    Write-Host "  Proceeding — esptool will fail with a clear error if port is unavailable."
}
Write-Host ""

# ─── SHA-256 pre-flight ────────────────────────────────────────────────────────
Write-Host "── Verifying firmware artifacts (SHA-256) ──" -ForegroundColor Yellow
if ($Mode -eq 'merged') {
    Assert-SHA256 -FilePath $MergedBin -ExpectedHash $KnownHashes[$MergedBin]
} else {
    Assert-SHA256 -FilePath $AppBin        -ExpectedHash $KnownHashes[$AppBin]
    Assert-SHA256 -FilePath $BootloaderBin -ExpectedHash $KnownHashes[$BootloaderBin]
    Assert-SHA256 -FilePath $PartTableBin  -ExpectedHash $KnownHashes[$PartTableBin]
}

# ─── Erase (explicit opt-in only) ──────────────────────────────────────────────
if ($Erase.IsPresent) {
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Red
    Write-Host "║  WARNING: -Erase will DESTROY all NVS and BLE pairing  ║" -ForegroundColor Red
    Write-Host "║  data. This cannot be undone.                           ║" -ForegroundColor Red
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Red
    Write-Host "  Port     : $Port" -ForegroundColor Yellow
    Write-Host "  Press Ctrl+C within 10 seconds to cancel."
    Start-Sleep -Seconds 10

    Write-Host ""
    Write-Host "  Running: esptool.py --chip esp32s3 --port $Port --baud $Baud erase_flash" -ForegroundColor Yellow
    & esptool.py --chip esp32s3 --port $Port --baud $Baud erase_flash
    if ($LASTEXITCODE -ne 0) {
        Write-Error "chip_erase FAILED (exit code $LASTEXITCODE)"
    }
    Write-Host "  Erase complete." -ForegroundColor Green
    Write-Host ""
}

# ─── Flash ─────────────────────────────────────────────────────────────────────
Write-Host "── Flashing — $Mode mode ──" -ForegroundColor Yellow
Write-Host "  Port : $Port"
if ($Mode -eq 'merged') {
    Write-Host "  Binary : merged-binary.bin (0x0)"
    Write-Host ""
    Write-Host "  Command:"
    Write-Host "  esptool.py --chip esp32s3 --port $Port --baud $Baud --before default_reset --after hard_reset write_flash 0x0 `"$MergedBin`""
    Write-Host ""

    & esptool.py `
        --chip esp32s3 `
        --port $Port `
        --baud $Baud `
        --before default_reset `
        --after hard_reset `
        write_flash `
        0x0 `
        "$MergedBin"
} else {
    Write-Host "  Binaries : bootloader.bin (0x0) + partition-table.bin (0x8000) + app.bin (0x10000)"
    Write-Host ""
    Write-Host "  Command:"
    Write-Host "  esptool.py --chip esp32s3 --port $Port --baud $Baud --before default_reset --after hard_reset write_flash --flash_mode dio --flash_freq 80m --flash_size 2MB 0x0 bootloader.bin 0x8000 partition-table.bin 0x10000 app.bin"
    Write-Host ""

    & esptool.py `
        --chip esp32s3 `
        --port $Port `
        --baud $Baud `
        --before default_reset `
        --after hard_reset `
        write_flash `
        --flash_mode dio `
        --flash_freq 80m `
        --flash_size 2MB `
        0x0     "$BootloaderBin" `
        0x8000  "$PartTableBin" `
        0x10000 "$AppBin"
}

$flashResult = $LASTEXITCODE

# ─── Result ────────────────────────────────────────────────────────────────────
Write-Host ""
if ($flashResult -eq 0) {
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║  FLASH: SUCCESS                                         ║" -ForegroundColor Green
    Write-Host "║  Record full output verbatim in FLASH-MANIFEST.md       ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Next step: .\monitor.ps1 -Port $Port" -ForegroundColor Cyan
    Write-Host "  Then:      Check Windows Device Manager (devmgmt.msc)" -ForegroundColor Cyan
} else {
    Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Red
    Write-Host "║  FLASH: FAILED (exit code: $flashResult)                      ║" -ForegroundColor Red
    Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Troubleshooting:" -ForegroundColor Yellow
    Write-Host "  1. Hold BOOT button on the board, then re-run this script"
    Write-Host "  2. Try lower baud:  .\flash.ps1 -Port $Port -Baud 460800"
    Write-Host "  3. Try -Erase if the previous flash was corrupt (destroys NVS)"
    Write-Host "  4. Verify USB cable supports data (not charge-only)"
    Write-Host "  5. Check driver: VID_303A = native USB, VID_10C4 = CP2102"
    Write-Host "  6. Verify esptool version: pip install --upgrade esptool"
    exit 1
}
