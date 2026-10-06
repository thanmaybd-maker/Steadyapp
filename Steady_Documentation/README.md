# Steady — Documentation Pack

Specification 2.0 · 6 October 2026

## Read these three documents

1. [Product Requirements Document](Steady_PRD.md): purpose, user journeys, scope, functional and quality requirements, acceptance criteria, source coverage and eight-stage delivery plan.
2. [Detailed Design Specification](Steady_Design_Document.md): merged visual direction, palettes and typography, five-tab navigation, screen flows, components, empty/error states and accessibility.
3. [Technical Stack and Architecture](Steady_Tech_Stack.md): source audit, native Android stack, data model, time engine, sensor boundaries, encryption, backup/restore and build verification.

[Source_Inventory.csv](Source_Inventory.csv) records every non-directory entry in the nine supplied ZIPs, plus four relevant standalone planning documents. It includes byte counts, SHA-256 hashes and a disposition for each file. Hashes identify the supplied bytes; they do not certify correctness or licensing.

## What was merged

- E: the Expo/React Native Steady project, especially routine anchors, Pocket Reset, Daybook styling and portable-file UX.
- N: the Kotlin/Compose AI Studio variant, including native screens, recipe editing, workout modes and proposal-flow ideas.
- S0–S6: seven Stitch screen exports and their identical shared design specification.
- A/B: the earlier merged Android architecture and blueprint prompt.
- U2/U3 and conversation: the expansion brief, supplied previous plan, lifetime use, Android focus, customization, accessible daily support and no buddy component.

## Central decisions

One native Kotlin/Compose application, with Today, Health, Focus, Review and Settings plus globally accessible Safety. Kinetic is the initial palette; Daybook is an optional appearance. Core records remain offline and encrypted. Public help must work independently of personal databases. Private Safety and contacts stay out of ordinary exports and backups. All activity values identify whether they were observed, entered or estimated.

Use the prototypes as source material, not as proof that these contracts are implemented. The audit found incompatible persistence/export behavior, simulated measurements, incomplete restore and timer accounting issues. The documents specify their replacement and verification.

## Review limits

This was a static review of application logic, configuration, supplied planning material and screen references. Every ZIP entry is inventoried; template files and assets were categorized, not all line-by-line security audited. No app build, test suite or device execution was performed for this documentation task. Current GitHub contents and installed-app identity were not reconciled. The earlier agent's M01–M08 claims are historical reports, not a fresh certification.

The technical document cites official references for platform choices. Candidate dependency versions still require a compatibility build. The exact OpenGym/Vital3D sources and reuse rights remain unresolved. Original archives are unchanged and are not duplicated in this pack.

## Use during implementation

Read the PRD first, then design and technical details together. Start at R0: reconcile the canonical repository, signing/schema history and build environment. Track acceptance by requirement ID and evidence, and retain deferred features in the backlog. Do not overwrite the existing repository with either uploaded project. This package contains specifications and an inventory, not an APK.
