param(
    [switch]$SkipIfUnavailable
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$nativeCoreRoot = Join-Path $projectRoot "native-core"
$jniLibsRoot = Join-Path $projectRoot "app\src\main\jniLibs"
$prebuiltRoot = Join-Path $nativeCoreRoot "prebuilt-libs"

function Ensure-Dir([string]$Path) {
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
}

function Copy-IfExists([string]$Source, [string]$DestinationDir) {
    if (Test-Path $Source) {
        Ensure-Dir $DestinationDir
        Copy-Item -Force $Source $DestinationDir
        return $true
    }
    return $false
}

Ensure-Dir $jniLibsRoot

$cargoCmd = Get-Command cargo -ErrorAction SilentlyContinue
$rustupCmd = Get-Command rustup -ErrorAction SilentlyContinue

if (-not $cargoCmd -or -not $rustupCmd) {
    Write-Host "Rust toolchain not found in PATH."
    Write-Host "Falling back to prebuilt native libraries if present."

    $copiedAny = $false
    foreach ($abi in @("arm64-v8a", "armeabi-v7a", "x86_64")) {
        $source = Join-Path $prebuiltRoot "$abi\libadaptive_overlay_core.so"
        $dest = Join-Path $jniLibsRoot $abi
        if (Copy-IfExists $source $dest) {
            $copiedAny = $true
            Write-Host "Packed prebuilt native library for $abi"
        }
    }

    if (-not $copiedAny) {
        $message = "No Rust toolchain and no prebuilt Android native libraries were found. JNI scaffold remains source-only."
        if ($SkipIfUnavailable) {
            Write-Host $message
            return
        }
        throw $message
    }

    return
}

$targets = @(
    @{ Abi = "arm64-v8a"; Triple = "aarch64-linux-android" },
    @{ Abi = "armeabi-v7a"; Triple = "armv7-linux-androideabi" },
    @{ Abi = "x86_64"; Triple = "x86_64-linux-android" }
)

Write-Host "Rust toolchain detected. Native Android build path is ready for target compilation."
Write-Host "This script currently expects the proper Android Rust targets and linker setup to be installed."
Write-Host "If they are already present, the next step is to extend this script with cargo target builds per ABI."

foreach ($target in $targets) {
    $dest = Join-Path $jniLibsRoot $target.Abi
    Ensure-Dir $dest
}

Write-Host "JNI packaging directories prepared:"
foreach ($target in $targets) {
    Write-Host " - $($target.Abi)"
}
