# Dependency gate

Updated 6 October 2026. Selected does not mean resolved or tested. The Windows
host has no JDK/SDK and all Gradle checks fail before Gradle starts.

## Candidate toolchain

| Component | Pin | Evidence/limit |
| --- | --- | --- |
| AGP | 8.6.1 | [Official compatibility](https://developer.android.com/build/releases/agp-8-6-0-release-notes): SDK 35, Gradle 8.7, minimum JDK 17. Not resolved here. |
| Gradle | 8.7 | Publisher SHA-256 pinned in wrapper; distribution not downloaded. |
| JDK | 17 | Prepared Windows Temurin 17.0.20.1+1 archive/checksum; not installed. |
| Kotlin/compiler | 1.9.22 / 1.5.10 | Existing pins; combined compilation pending. |
| Compose BOM | 2024.02.02 | Existing pin retained; not presented as newest. |
| Room / KSP | 2.6.1 / 1.9.22-1.0.17 | Schema export path configured; generation pending. |
| SDK | compile/target 35, minimum 26 | SDK absent; device compatibility untested. |
| SQLCipher | net.zetetic:sqlcipher-android:4.9.0 | Replaces end-of-life legacy 4.5.4. Tagged Room API consulted; artifact audit pending. |

Other retained library pins are listed in `gradle/libs.versions.toml`. The architecture's
proposed newer toolchain was never proved. This repairs the imported scaffold's
compatibility mismatch; it does not claim the combined candidate builds.

## SQLCipher review

[Tagged integration documentation](https://github.com/sqlcipher/sqlcipher-android/tree/v4.9.0)
documents `SupportOpenHelperFactory` and explicit native library loading.
[Zetetic's guidance](https://www.zetetic.net/blog/2025/06/26/sqlcipher-for-android-16kb-page-size-support/)
states modern-library 16-KB support from 4.6.1 and legacy community-library end of
life. Actual packaged ABI/page alignment and encrypted-file behaviour remain checks.

The [tagged licence](https://github.com/sqlcipher/sqlcipher-android/blob/v4.9.0/LICENSE)
requires preserved redistribution notices. Exact Maven artifact provenance and full
native/transitive notices remain pending. AndroidX/Kotlin/JUnit resolved-artifact
licences must also be audited before packaging; the former document did not do so.

## Permission gate

Source manifest requests notifications, vibration and SCHEDULE_EXACT_ALARM.
USE_EXACT_ALARM was removed; no eligibility assertion is made. No INTERNET
permission appears in the source manifest. Merged manifest generation/audit and
timer permission/fallback flows are still pending.
