param(
    [ValidateSet('probe', 'read', 'proof')]
    [string]$Action = 'probe',
    [string]$Serial
)

$ErrorActionPreference = 'Stop'
$nr = 1L -shl 19
$lte = 1L -shl 12
$bits = [ordered]@{
    GPRS=0; EDGE=1; UMTS=2; CDMA=3; EVDO_0=4; EVDO_A=5; '1xRTT'=6
    HSDPA=7; HSUPA=8; HSPA=9; IDEN=10; EVDO_B=11; LTE=12; EHRPD=13
    'HSPA+'=14; GSM=15; TD_SCDMA=16; IWLAN=17; LTE_CA=18; NR=19
}

if (-not $Serial) {
    $devices = @(& adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '^([^\s]+)\s+device$' } |
        ForEach-Object { $Matches[1] } | Where-Object { $_ -notmatch '^emulator-' })
    if ($devices.Count -ne 1) { throw "Expected one physical ADB device, found $($devices.Count). Pass -Serial if needed." }
    $Serial = $devices[0]
}
$script:adbArgs = @('-s', $Serial)

function Invoke-AdbShell([Parameter(ValueFromRemainingArguments=$true)][string[]]$Arguments) {
    $result = & adb @script:adbArgs shell @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb shell failed: $($result -join "`n")" }
    return ($result -join "`n").Trim()
}

function Get-Target {
    $dump = Invoke-AdbShell @('dumpsys', 'isub')
    $active = [regex]::Match($dump, '(?m)^activeDataSubId=(\d+)\s*$')
    if (-not $active.Success) { throw 'Could not resolve activeDataSubId' }
    $subId = [int]$active.Groups[1].Value
    $slot = [regex]::Match($dump, "(?m)^\s*Logical SIM slot (\d+): subId=$subId\s*$")
    if (-not $slot.Success) { throw "Could not map active data subscription $subId to a logical slot" }
    [pscustomobject]@{ SubId=$subId; Slot=[int]$slot.Groups[1].Value }
}

function Convert-AllowedTypesToMask([string]$Text) {
    if ($Text -notmatch '^[A-Za-z0-9_+]+(?:\|[A-Za-z0-9_+]+)*$') {
        throw "Unexpected allowed-types output: $Text"
    }
    $names = $Text.Split('|')
    if (($names | Select-Object -Unique).Count -ne $names.Count) { throw 'Duplicate network type in output' }
    $mask = 0L
    foreach ($name in $names) {
        if (-not $bits.Contains($name)) { throw "Unknown network type: $name" }
        $mask = $mask -bor (1L -shl $bits[$name])
    }
    if ($mask -le 0 -or ($mask -band $lte) -eq 0) { throw 'Refusing empty policy or policy without LTE' }
    return $mask
}

function Read-Policy($Target) {
    $text = Invoke-AdbShell @('cmd', 'phone', 'get-allowed-network-types-for-users', '-s', "$($Target.Slot)")
    [pscustomobject]@{ Text=$text; Mask=(Convert-AllowedTypesToMask $text) }
}

function Set-Policy($Target, [long]$Mask) {
    $binary = [Convert]::ToString($Mask, 2)
    $result = Invoke-AdbShell @('cmd', 'phone', 'set-allowed-network-types-for-users', '-s', "$($Target.Slot)", $binary)
    if ($result -ne 'set-allowed-network-types-for-users completed') { throw "Set was not confirmed: $result" }
}

$target = Get-Target
switch ($Action) {
    'probe' {
        "Model: $(Invoke-AdbShell @('getprop','ro.product.model'))"
        "Android: $(Invoke-AdbShell @('getprop','ro.build.version.release')) API $(Invoke-AdbShell @('getprop','ro.build.version.sdk'))"
        "Build: $(Invoke-AdbShell @('getprop','ro.build.version.incremental'))"
        "Active data: subId=$($target.SubId), slot=$($target.Slot)"
        Invoke-AdbShell @('cmd', 'phone', 'help')
    }
    'read' {
        $policy = Read-Policy $target
        "Active data: subId=$($target.SubId), slot=$($target.Slot)"
        "Mask: $($policy.Mask)"
        "Types: $($policy.Text)"
    }
    'proof' {
        $original = Read-Policy $target
        if (($original.Mask -band $nr) -eq 0) { throw 'Original policy has NR disabled; enable 5G in Settings before this two-way proof' }
        $fourG = $original.Mask -band (-bnot $nr)
        "Original mask: $($original.Mask)"
        "Original types: $($original.Text)"
        try {
            $fresh = Get-Target
            if ($fresh.SubId -ne $target.SubId -or $fresh.Slot -ne $target.Slot) { throw 'Mobile-data SIM changed before write' }
            Set-Policy $target $fourG
            Start-Sleep -Milliseconds 350
            $after4 = Read-Policy $target
            if ($after4.Mask -ne $fourG) { throw "4G verification failed: expected $fourG, got $($after4.Mask)" }
            "4G read-back: $($after4.Mask) $($after4.Text)"
        } finally {
            Set-Policy $target $original.Mask
            Start-Sleep -Milliseconds 350
            $restored = Read-Policy $target
            if ($restored.Mask -ne $original.Mask) { throw "Restore verification failed: expected $($original.Mask), got $($restored.Mask)" }
            "5G/restore read-back: $($restored.Mask) $($restored.Text)"
        }
    }
}
