# AGENTS.md

### Context and rule
Build Steady for one user. Keep v0.1 to Today, Break, Safety, Review, Settings. Respect the improvement guide, this architecture, and the user’s current instruction. Inspect first, make the smallest coherent change, verify it, and report what is still uncertain. If blocked, name the exact blocker instead of rewriting the project.

### Conventions
- One app module; ui/domain/data/platform/di package boundaries. Immutable screen state, injected clock, repository source of truth, manual DI. Keep user-visible strings in resources. Preserve draft input and existing records on error.
- No internet, telemetry, medical logic, personality scoring, guilt mechanics, repository copies, or real personal fixtures. Check dependency permissions/licences. Never include private Safety fields in export, backup, logs, prompts or screenshots.
- Use encrypted Room storage; plaintext DataStore only for non-personal bootstrap. No destructive migration fallback, swallowed save failure, hard-coded keys, custom cipher, or unverified dependency/API.
- All essential controls labelled and at least 56 dp; maximum text scale, TalkBack order, contrast, insets and non-visual alternatives. Safety remains reachable; public help does not require the database or authentication.

### Verification and definition of done
Run the applicable Gradle commands listed on the preceding page. Do not report an unavailable command as passed. Use deterministic unit checks for rules and instrumented/device checks for storage/platform behaviour. A milestone is done when its criterion passes, previous relevant checks remain green, and docs/evidence/mNN.md records commit, commands, results, screenshots/test settings, limits and next action.

Before a substantial change, preserve a working checkpoint. Use synthetic data. No automatic posting, purchases, contact actions, publishing, or additional scope. If a dependency spike blocks progress, stop that feature and report evidence; do not remove privacy/accessibility requirements to force a pass.
