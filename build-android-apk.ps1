param(
    [switch]$Release
    ,[switch]$Offline
    ,[switch]$VerboseLog
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
$androidSdk = "C:\Users\vlado\AppData\Local\Android\Sdk"
$toolsDir = Join-Path $projectRoot ".tools"
$gradleVersion = "8.9"
$gradleZip = Join-Path $toolsDir "gradle-$gradleVersion-bin.zip"
$gradleHome = Join-Path $toolsDir "gradle-$gradleVersion"
$gradleBin = Join-Path $gradleHome "bin\gradle.bat"

if (-not (Test-Path $androidStudioJbr)) {
    throw "Android Studio JBR not found: $androidStudioJbr"
}

if (-not (Test-Path $androidSdk)) {
    throw "Android SDK not found: $androidSdk"
}
$moduleBuild = Join-Path $projectRoot "app\build.gradle"
$moduleText = Get-Content -Raw $moduleBuild
$match = [regex]::Match($moduleText, "compileSdk\s+(\d+)")
if (-not $match.Success) {
    throw "compileSdk not found in app/build.gradle"
}
$compileSdk = $match.Groups[1].Value
$platformDir = Join-Path $androidSdk ("platforms\\android-$compileSdk")
$platformPkg = Join-Path $platformDir 'package.xml'
if (-not (Test-Path $platformPkg)) {
    throw "Android platform target android-$compileSdk not found in SDK: $platformDir. Run SDK Manager: sdkmanager --install 'platforms;android-$compileSdk'"
}

New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

if (-not (Test-Path $gradleBin)) {
    if (-not (Test-Path $gradleZip)) {
        Write-Host "Downloading Gradle $gradleVersion..."
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $gradleZip
    }

    Write-Host "Expanding Gradle..."
    Expand-Archive -Path $gradleZip -DestinationPath $toolsDir -Force
    if (-not (Test-Path (Join-Path $toolsDir "gradle-$gradleVersion"))) {
        $extracted = Get-ChildItem $toolsDir -Directory | Where-Object { $_.Name -like "gradle-*" } | Select-Object -First 1
        if ($extracted -and $extracted.Name -ne "gradle-$gradleVersion") {
            Rename-Item -Path $extracted.FullName -NewName "gradle-$gradleVersion"
        }
    }
}

$env:JAVA_HOME = $androidStudioJbr
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk
$env:Path = "$androidStudioJbr\bin;$env:Path"

$nativePrepScript = Join-Path $projectRoot "build-native-core.ps1"
if (Test-Path $nativePrepScript) {
    Write-Host "Preparing Rust native-core packaging..."
    & $nativePrepScript -SkipIfUnavailable
}

$task = if ($Release) { "assembleRelease" } else { "assembleDebug" }
$apkVariant = if ($Release) { "release" } else { "debug" }
$gradleArgs = @($task, "--stacktrace")
if ($Offline) { $gradleArgs += "--offline" }
if ($VerboseLog) { $gradleArgs += "--info" }

Push-Location $projectRoot
try {
    & $gradleBin @gradleArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

$outputDir = Join-Path $projectRoot "app\build\outputs\apk\$apkVariant"
$apk = Get-ChildItem -Path $outputDir -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($apk) {
    Write-Host "APK generated: $($apk.FullName)"
    Write-Host "Size: $([Math]::Round($apk.Length / 1MB, 2)) MB"
} else {
    Write-Warning "Build finished but APK was not found in: $outputDir"
}

