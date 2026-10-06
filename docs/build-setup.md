# Windows build setup

The current host has no discoverable Java installation or Android SDK. The Gradle
wrapper exits with code 1 before Gradle starts. The old Linux hang is not a diagnosis
of this Windows host.

The prepared script installs tools only under the ignored project `.tools/`
directory. It makes no machine-wide PATH change and creates no project copy.
It verifies archives before extraction and stops on failed setup or Gradle commands.
Installing the Android SDK requires agreement to Google's
[SDK terms](https://developer.android.com/studio#command-tools).
Without the explicit acceptance parameter the script does not install tools or
accept terms. The script has been parsed, but installation has not been run.

After agreeing to the terms:

```powershell
.\scripts\build.ps1 -InstallTools -AcceptAndroidSdkLicense
```

With existing tools, set JAVA_HOME to JDK 17 and ANDROID_HOME to the Android SDK,
then run ` .\scripts\build.ps1 ` without install flags. Android Studio's SDK Manager
is an alternative for installing platform 35 and build-tools 34.0.0.

Add `-ConnectedTests` after connecting an authorised Android device or starting an
emulator. Installation, synthetic storage tests, maximum font/TalkBack, Doze,
credential lock, recovery and primary-phone acceptance remain required.

## Verified download metadata, 6 October 2026

- Gradle 8.7 SHA-256: `544c35d6bd849ae8a5ed0bcea39ba677dc40f49df7d1835561582da2009b961d`, from [Gradle](https://services.gradle.org/distributions/gradle-8.7-bin.zip.sha256).
- Temurin JDK 17.0.20.1+1 Windows x64 SHA-256: `e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0`, from [Adoptium release metadata](https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse).
- Android command-line tools 12.0, Windows archive `commandlinetools-win-11076708_latest.zip`: publisher SHA-1 `3d2917302740f476999a091bc5558837c7a863c5`, from [Google SDK repository metadata](https://dl.google.com/android/repository/repository2-1.xml). SHA-1 here checks the publisher's download metadata; it is not an application encryption choice.

These are metadata checks, not evidence of installation or a passing build.
