param(
    [switch]$SkipIfUnavailable
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$nativeCoreRoot = Join-Path $projectRoot "native-core"
$jniLibsRoot = Join-Path $projectRoot "app\src\main\jniLibs"
$prebuiltRoot = Join-Path $nativeCoreRoot "prebuilt-libs"
$sdkRoot = "C:\Users\vlado\AppData\Local\Android\Sdk"
$ndkRootBase = Join-Path $sdkRoot "ndk"
$cargoBin = Join-Path $env:USERPROFILE ".cargo\bin"

if (Test-Path $cargoBin) {
    $env:Path = "$cargoBin;$env:Path"
}

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
$cargoExe = Get-Command cargo -ErrorAction SilentlyContinue
$cargoNdkCmd = Get-Command cargo-ndk -ErrorAction SilentlyContinue

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

if (-not (Test-Path $ndkRootBase)) {
    $message = "Android NDK directory not found: $ndkRootBase"
    if ($SkipIfUnavailable) {
        Write-Host $message
        return
    }
    throw $message
}

$ndkDir = Get-ChildItem $ndkRootBase -Directory | Sort-Object Name -Descending | Select-Object -First 1
if (-not $ndkDir) {
    $message = "No installed Android NDK version found under $ndkRootBase"
    if ($SkipIfUnavailable) {
        Write-Host $message
        return
    }
    throw $message
}

if (-not $cargoExe -or -not $cargoNdkCmd) {
    $message = "cargo and/or cargo-ndk are not installed or not in PATH."
    if ($SkipIfUnavailable) {
        Write-Host $message
        return
    }
    throw $message
}

$env:ANDROID_NDK_HOME = $ndkDir.FullName
$env:ANDROID_NDK_ROOT = $ndkDir.FullName
$env:ANDROID_SDK_ROOT = $sdkRoot

foreach ($abi in @("arm64-v8a", "armeabi-v7a", "x86_64")) {
    Ensure-Dir (Join-Path $jniLibsRoot $abi)
}

Write-Host "Building Rust native-core with cargo-ndk..."
Write-Host "NDK: $($ndkDir.FullName)"

Push-Location $nativeCoreRoot
try {
    & $cargoExe.Source ndk `
        -t arm64-v8a `
        -t armeabi-v7a `
        -t x86_64 `
        -o $jniLibsRoot `
        build --release
    if ($LASTEXITCODE -ne 0) {
        throw "cargo-ndk build failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Write-Host "Rust Android libraries packaged into jniLibs:"
Get-ChildItem $jniLibsRoot -Recurse -Filter "libadaptive_overlay_core.*" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host " - $($_.FullName)"
}
