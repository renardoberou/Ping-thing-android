# The Ping Thing — Android

**Resonant Systems** · *Controlled systems. Unpredictable sound.*

Native Android packaging of **The Ping Thing**, a generative spatial sequencer: a radar field where enemies are voices, position is timbre and space, and a defender hunts the patterns you build. Four physically-modelled resonators, binaural HRTF spatialization, Euclidean percussion clustered across the whole field, a warm 3D spatial delay, a three-ring reactive oscilloscope, a kick that physically repels the field, and a mastering chain targeting −14 LUFS — one HTML instrument, packaged as a Kotlin WebView shell built entirely in GitHub Actions.

## Status — Phases 0–2 shipped and device-confirmed. Phase 3 infrastructure shipped; first signed release not yet cut.

| Phase | State |
|---|---|
| 0 — Planning | ✅ Done. Plan, ADRs, docs approved. |
| 1 — Minimum viable APK | ✅ Done. **Confirmed on owner's device**: "opens and works perfectly" after a launch-crash regression was caught and fixed (crash-visibility scaffolding is now permanent — see `docs/PHASE-CHECKLISTS.md`). |
| 2 — Native integration | ✅ Shipped: AndroidHost JS bridge, audio focus, `BACKGROUND AUDIO` foreground service, haptics on BOMB/FIRE, Stage Mode (radar-only view for TV mirroring, e.g. Samsung Smart View). Gate closed by owner instruction; the 10-minute screen-off soak and music-app focus check from `PLAN.md` §5 were not individually itemised back — treat as **unverified in detail** until run. |
| 3 — Release engineering | 🟡 Infrastructure shipped (env-driven signing in `app/build.gradle.kts`, `.github/workflows/release.yml` triggers signed APK+AAB on tag `v*`). **No keystore has been generated and no `v*` tag has been pushed yet.** This is the current blocking step — see `docs/RELEASE.md`. |
| 4 — Native enhancements | Not started (native MIDI, native recorder, Oboe percussion — all demand-driven, see `PLAN.md` §7). |

Full gate-by-gate detail and session history: **[`docs/PHASE-CHECKLISTS.md`](docs/PHASE-CHECKLISTS.md)**.

## Getting a build right now
No signed release exists yet. Debug APKs are produced on every push to `main`:
1. Actions tab → latest **build** workflow run → Artifacts → `ping-thing-debug-apk`.
2. Download (browser, or via the GitHub API from Termux if the web UI is misbehaving — see `docs/RELEASE.md`), unzip, install.
3. Debug and future signed-release builds have different app signatures — Android will not upgrade one into the other. Export your preset bank (in-app **↓ SAVE**) before switching between them.

## Known limitations (current, not hypothetical)
- **Web MIDI does not exist in Android WebView.** MIDI-learn and hardware MIDI controllers do not work in the app (they do in a desktop browser). A native `android.media.midi` bridge is scoped as Phase 4, demand-driven.
- **REC button (MediaRecorder) behaviour inside the WebView has not been explicitly verified** on-device. If it fails, a native recorder fallback is scoped in Phase 4.
- **No automated test suite.** All verification to date is manual: owner installs the artifact APK and confirms behaviour directly. There is no CI-run unit/instrumentation test step.
- Portrait-locked; no tablet/landscape layout.

## Map
| File | What it is |
|---|---|
| [`PLAN.md`](PLAN.md) | Master build plan: architecture, phases, gates, risks |
| [`web/ping-thing.html`](web/ping-thing.html) | **The instrument.** Single source of truth. Runs in any browser today, and is the exact file bundled into the Android app's assets at build time |
| [`android/`](android/) | The Kotlin WebView shell — `MainActivity` (bridge, focus, lifecycle, crash-visibility), `PlaybackService` (background-audio foreground service) |
| [`docs/AGENT_GUIDE.md`](docs/AGENT_GUIDE.md) | How agents/contributors work here — read before touching anything |
| [`docs/ADR-001-architecture.md`](docs/ADR-001-architecture.md) | Why WebView-first, alternatives considered |
| [`docs/PHASE-CHECKLISTS.md`](docs/PHASE-CHECKLISTS.md) | Live gates + full session log — the definitive record of what has and hasn't been verified |
| [`docs/RELEASE.md`](docs/RELEASE.md) | Signing & release runbook (phone-only procedures; **read before generating a keystore**) |

## Build philosophy
Zero local tooling. Every APK or AAB this repo ever produces is built by GitHub Actions from a clean checkout, and installed straight from the Actions/Releases page onto a phone. No keystore, secret, `.env`, or binary build output is ever committed to this repository.

---
© Resonant Systems. All rights reserved.
