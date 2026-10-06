# Resolved dependencies and redistribution notices

Updated 6 October 2026. The local Windows toolchain builds the existing application.
The previous JDK/SDK-unavailable audit is superseded by r0/r6/r7 evidence.

| Component | Verified pin |
| --- | --- |
| JDK | Local Temurin 17.0.20.1+1 |
| Gradle / AGP | 8.7 (distribution checksum pinned) / 8.6.1 |
| Kotlin / Compose compiler | 1.9.22 / 1.5.10 |
| Compose BOM | 2024.02.02 |
| Room / KSP | 2.6.1 / 1.9.22-1.0.17 |
| Android | compile/target 35, minimum 26; Xiaomi tested on actual API 36 |
| SQLCipher Android | net.zetetic:sqlcipher-android:4.9.0, Community |
| Serialization / DataStore | 1.6.2 / 1.1.1 |

The proven combination is retained under Tech Stack §4's compatibility-spike rule;
it is not presented as the newest available stack. Other pins are in the version
catalog. Dependency upgrade requires its own encrypted migration/native/device checks.

## Inventory and licences

[Runtime inventory](runtime-inventory.csv) records 83 resolved artifacts and their
SHA-256 artifact/POM hashes. Regenerate with:

```powershell
.\gradlew.bat :app:writeRuntimeInventory -I scripts/runtime-inventory.init.gradle --no-daemon
```

Eighty-one POMs declare Apache 2.0. Two need additional source provenance:

- Guava ListenableFuture 1.0 inherits its parent; its tagged
  [source notice](https://github.com/google/guava/blob/v26.0/guava/src/com/google/common/util/concurrent/ListenableFuture.java)
  declares Apache 2.0 and copyright 2007 The Guava Authors.
- SQLCipher's POM supplies the vendor licence URL without a named licence.
  Its [Android 4.9.0 licence](https://github.com/sqlcipher/sqlcipher-android/blob/v4.9.0/LICENSE)
  and [core 4.9.0 licence](https://github.com/sqlcipher/sqlcipher/blob/v4.9.0/LICENSE.md)
  contain the BSD redistribution notices bundled in the APK.

The packaged arm64 SQLCipher binary identifies OpenSSL 3.0.16 (11 February 2025).
Its [tagged licence](https://github.com/openssl/openssl/blob/openssl-3.0.16/LICENSE.txt)
is bundled alongside Apache 2.0, SQLCipher notices and the inventory in assets/licenses.
Settings exposes these offline. No imported fonts, audio, exercise imagery or
third-party UI artwork is currently shipped. Imported prototype references do not
establish permission to reuse assets; OpenGym/Vital3D still require exact sources.

AAR/JAR notice scanning found no additional top-level LICENSE/NOTICE/COPYRIGHT entries.
This is an observed archive scan, not a claim that unnamed native dependencies are
licensed by a POM. Native provenance above was checked separately.

## Permission and native gates

The offline source and previously generated manifest have no INTERNET permission.
They request notifications, vibration and SCHEDULE_EXACT_ALARM. Exact scheduling
requires the actual platform grant; otherwise cues disclose possible delay. No
location/sensor/usage/accessibility service is enabled in this checkpoint.

The native alignment script checks real ZIP entry offsets/storage method and ELF
LOAD alignment without requiring unsupported zipalign 34 -P syntax. All eight
libraries across four ABIs passed for the tested r7 APK. Re-run on each final APK;
that result does not substitute for actual install/update and performance evidence.
