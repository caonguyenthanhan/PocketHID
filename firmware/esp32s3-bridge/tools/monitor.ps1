<#
.SYNOPSIS
    PocketHID ESP32-S3 Serial Monitor — Wave 9R
    Opens serial monitor and captures boot log for physical validation.

.DESCRIPTION
    Monitors ESP32-S3 serial output. Verifies that the boot sequence matches
    expected Wave 8 firmware initialization order.

    Expected boot sequence markers (in order):
      1. "Booting PocketHID"          — application entry
      2. "NVS initialized"            — NVS partition OK
      3. "BLE stack initialized"      — BLE stack OK
      4. "USB stack initialized"      — TinyUSB HID stack OK
      5. "Safety manager initialized" — watchdog active
      6. "Ready."                     — system ready for packets

.PARAMETER Port
    Serial port (e.g., COM3). Required.

.PARAMETER Baud
    Monitor baud rate. Default: 115200.

.PARAMETER Duration
    Capture duration in seconds. Default: 30 (captures full boot).
    Use 0 for indefinite monitoring (Ctrl+C to stop).

.PARAMETER LogFile
    Path to save captured output. Default: .\monitor-YYYY-MM-DD-HHmmss.log

.EXAMPLE
    # Basic boot capture (30 seconds)
    .\monitor.ps1 -Port COM3

    # Extended monitoring with log file
    .\monitor.ps1 -Port COM3 -Duration 60 -LogFile ".\boot-log.txt"

    # Indefinite monitoring
    .\monitor.ps1 -Port COM3 -Duration 0

.NOTES
    Wave 9R — Physical Validation Gate
    Copy captured boot log verbatim into docs/spikes/USB-HID-PHYSICAL-RESULTS.md
    Boot VERIFICATION requires ALL 6 markers present.
    "USB stack initialized" ≠ "Windows HID enumerated" — they are SEPARATE steps.
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$Port,

    [int]$Baud = 115200,

    [int]$Duration = 30,

    [string]$LogFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ─── Setup log file ──────────────────────────────────────────────────────────────
if ($LogFile -eq "") {
    $timestamp = Get-Date -Format "yyyy-MM-dd-HHmmss"
    $LogFile = ".\monitor-$timestamp.log"
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   PocketHID ESP32-S3 Monitor — Wave 9R               ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "Port     : $Port"
Write-Host "Baud     : $Baud"
Write-Host "Duration : $(if ($Duration -eq 0) { 'indefinite (Ctrl+C to stop)' } else { "$Duration seconds" })"
Write-Host "Log file : $LogFile"
Write-Host ""

# ─── Expected boot markers ───────────────────────────────────────────────────────
$ExpectedMarkers = @(
    "Booting PocketHID",
    "NVS initialized",
    "BLE stack initialized",
    "USB stack initialized",
    "Safety manager initialized",
    "Ready."
)
$FoundMarkers = @{}
foreach ($m in $ExpectedMarkers) { $FoundMarkers[$m] = $false }

# ─── Open serial port ────────────────────────────────────────────────────────────
try {
    $serial = New-Object System.IO.Ports.SerialPort
    $serial.PortName    = $Port
    $serial.BaudRate    = $Baud
    $serial.DataBits    = 8
    $serial.Parity      = [System.IO.Ports.Parity]::None
    $serial.StopBits    = [System.IO.Ports.StopBits]::One
    $serial.ReadTimeout = 1000
    $serial.Open()
} catch {
    Write-Error "Failed to open $Port : $($_.Exception.Message)`nEnsure the board is connected and no other tool owns the port."
}

Write-Host "Serial port opened. Monitoring..." -ForegroundColor Green
Write-Host "─────────────────────────────────────────────────────────" -ForegroundColor Gray

$startTime = Get-Date
$logLines  = @()

try {
    while ($true) {
        # Duration check
        if ($Duration -gt 0) {
            $elapsed = (Get-Date) - $startTime
            if ($elapsed.TotalSeconds -ge $Duration) {
                Write-Host ""
                Write-Host "Duration reached ($Duration s). Stopping monitor." -ForegroundColor Yellow
                break
            }
        }

        try {
            $line = $serial.ReadLine().TrimEnd()
            $ts   = (Get-Date -Format "HH:mm:ss.fff")
            $logLine = "[$ts] $line"

            # Print to console
            Write-Host $logLine

            # Append to log buffer
            $logLines += $logLine

            # Check for expected markers
            foreach ($marker in $ExpectedMarkers) {
                if ($line -match [regex]::Escape($marker)) {
                    $FoundMarkers[$marker] = $true
                    Write-Host "  ✓ MARKER FOUND: $marker" -ForegroundColor Green
                }
            }

            # Check for unexpected input injection at boot
            if ($line -match "HID report sent" -and (((Get-Date) - $startTime).TotalSeconds -lt 5)) {
                Write-Warning "SAFETY CONCERN: HID report detected within 5s of boot!"
            }

        } catch [System.TimeoutException] {
            # No data — continue loop
        }
    }
} finally {
    $serial.Close()

    # Save log
    $logLines | Out-File -FilePath $LogFile -Encoding UTF8
    Write-Host ""
    Write-Host "Log saved to: $LogFile" -ForegroundColor Cyan

    # Boot verification summary
    Write-Host ""
    Write-Host "══ BOOT VERIFICATION SUMMARY ══" -ForegroundColor Yellow
    $allFound = $true
    foreach ($marker in $ExpectedMarkers) {
        $status = if ($FoundMarkers[$marker]) { "FOUND   ✓" } else { "MISSING ✗" }
        $color  = if ($FoundMarkers[$marker]) { "Green" } else { "Red" }
        Write-Host "  [$status]  $marker" -ForegroundColor $color
        if (-not $FoundMarkers[$marker]) { $allFound = $false }
    }

    Write-Host ""
    if ($allFound) {
        Write-Host "BOOT VERIFICATION: PASS" -ForegroundColor Green
        Write-Host "Next step: Check Windows Device Manager for USB HID enumeration." -ForegroundColor Cyan
        Write-Host "  USB HID enumeration is SEPARATE from USB stack initialization."
    } else {
        Write-Host "BOOT VERIFICATION: INCOMPLETE" -ForegroundColor Red
        Write-Host "  Copy the full log to docs/spikes/USB-HID-PHYSICAL-RESULTS.md"
        Write-Host "  and record which markers are missing as FAIL evidence."
    }
}
