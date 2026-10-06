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

### Current owner instructions (7 October 2026)
- Continue the existing checkpoint. Implement the four `docs/steady-og-*` documents, reconciling their plans against actual source and verification evidence.
- Keep Kinetic and Daybook themes and customization. Adapt OG's richer cards, graphs, water and habit animations and interactions into those themes. Use theme-aware text colors; do not carry over fixed black/blue text or remove existing records/accessibility.
- Make the smallest coherent change. After each change batch, show its diff. Run applicable checks before reporting success and include their actual output.
- Never describe a task or feature as done merely because a file exists or compiles. Completion requires the implemented behavior and its applicable passing checks; record remaining gaps honestly.
- If an API is unknown, say so and consult Context7 when available. If it is unavailable, disclose that limitation and verify against primary documentation or installed source instead of guessing.
- These rules are repository-scoped working memory. The owner has also selected optional Gemini with their own API key; keep the offline core private and isolate any explicitly previewed, opt-in connected requests.
