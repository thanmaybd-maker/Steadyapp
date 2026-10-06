[CmdletBinding()]
param(
    [switch]$InstallTools,
    [switch]$AcceptAndroidSdkLicense,
    [switch]$ConnectedTests
)

$ErrorActionPreference = 'Stop'
$steadyProjectRoot = Split-Path -Parent $PSScriptRoot
$steadyToolsRoot = Join-Path $steadyProjectRoot '.tools'
$steadyJdkRoot = Join-Path $steadyToolsRoot 'jdk-17.0.20.1+1'
$steadySdkRoot = Join-Path $steadyToolsRoot 'android-sdk'

function Get-SteadyVerifiedArchive {
    param([string]$Uri, [string]$Path, [string]$Algorithm, [string]$Checksum)
    if (-not (Test-Path -LiteralPath $Path)) {
        Invoke-WebRequest -Uri $Uri -OutFile $Path -TimeoutSec 600
    }
    $steadyActualHash = (Get-FileHash -LiteralPath $Path -Algorithm $Algorithm).Hash
    if ($steadyActualHash -ne $Checksum) {
        throw "Checksum mismatch for $Path. Archive was retained for inspection; it was not extracted."
    }
}

if ($InstallTools) {
    # Consent is explicit; running the script without this flag accepts no terms.
    if (-not $AcceptAndroidSdkLicense) {
        throw 'Read https://developer.android.com/studio#command-tools and explicitly pass -AcceptAndroidSdkLicense before installing the SDK.'
    }
    New-Item -ItemType Directory -Path $steadyToolsRoot -Force | Out-Null
    if (-not (Test-Path -LiteralPath (Join-Path $steadyJdkRoot 'bin/java.exe'))) {
        $steadyJdkZip = Join-Path $steadyToolsRoot 'temurin17.zip'
        Get-SteadyVerifiedArchive `
            -Uri 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip' `
            -Path $steadyJdkZip -Algorithm SHA256 `
            -Checksum 'e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0'
        Expand-Archive -LiteralPath $steadyJdkZip -DestinationPath $steadyToolsRoot -Force
    }
    $env:JAVA_HOME = $steadyJdkRoot
    $steadySdkManager = Join-Path $steadySdkRoot 'cmdline-tools/12.0/bin/sdkmanager.bat'
    if (-not (Test-Path -LiteralPath $steadySdkManager)) {
        $steadySdkZip = Join-Path $steadyToolsRoot 'commandlinetools-win-11076708.zip'
        Get-SteadyVerifiedArchive `
            -Uri 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' `
            -Path $steadySdkZip -Algorithm SHA1 `
            -Checksum '3d2917302740f476999a091bc5558837c7a863c5'
        $steadySdkStaging = Join-Path $steadyToolsRoot 'sdk-staging'
        Expand-Archive -LiteralPath $steadySdkZip -DestinationPath $steadySdkStaging -Force
        $steadySdkTarget = [IO.Path]::GetFullPath((Join-Path $steadySdkRoot 'cmdline-tools/12.0'))
        $steadyStagingSource = [IO.Path]::GetFullPath((Join-Path $steadySdkStaging 'cmdline-tools'))
        $steadyCheckedRoot = [IO.Path]::GetFullPath($steadyToolsRoot) + [IO.Path]::DirectorySeparatorChar
        if (-not $steadySdkTarget.StartsWith($steadyCheckedRoot, [StringComparison]::OrdinalIgnoreCase) -or
            -not $steadyStagingSource.StartsWith($steadyCheckedRoot, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'SDK extraction paths must stay within the project tool directory.'
        }
        New-Item -ItemType Directory -Path (Split-Path -Parent $steadySdkTarget) -Force | Out-Null
        Move-Item -LiteralPath $steadyStagingSource -Destination $steadySdkTarget
    }
    # Google SDK licence acceptance is gated by the explicit parameter above.
    1..20 | ForEach-Object { 'y' } | & $steadySdkManager "--sdk_root=$steadySdkRoot" --licenses
    if ($LASTEXITCODE -ne 0) { throw "SDK licence command failed: $LASTEXITCODE" }
    & $steadySdkManager "--sdk_root=$steadySdkRoot" 'platform-tools' 'platforms;android-35' 'build-tools;34.0.0'
    if ($LASTEXITCODE -ne 0) { throw "SDK installation failed: $LASTEXITCODE" }
}

if (Test-Path -LiteralPath (Join-Path $steadyJdkRoot 'bin/java.exe')) {
    $env:JAVA_HOME = $steadyJdkRoot
}
if (Test-Path -LiteralPath (Join-Path $steadySdkRoot 'platforms/android-35/android.jar')) {
    $env:ANDROID_HOME = $steadySdkRoot
}
if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'JDK is missing. Install JDK 17 or run with -InstallTools -AcceptAndroidSdkLicense after agreeing to the SDK licence.'
}
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT -and
    -not (Test-Path -LiteralPath (Join-Path $steadyProjectRoot 'local.properties'))) {
    throw 'Android SDK is missing. Set ANDROID_HOME or configure local.properties.'
}

Push-Location $steadyProjectRoot
try {
    $steadyTasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug')
    if ($ConnectedTests) { $steadyTasks += ':app:connectedQaAndroidTest' }
    & (Join-Path $steadyProjectRoot 'gradlew.bat') @steadyTasks --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle checks failed: $LASTEXITCODE" }
} finally {
    Pop-Location
}
