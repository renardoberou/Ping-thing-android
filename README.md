# The Ping Thing — Android

**Resonant Systems** · *Controlled systems. Unpredictable sound.*

Native Android packaging of **The Ping Thing**, a generative spatial sequencer: a radar field where enemies are voices, position is timbre and space, and a defender hunts the patterns you build. Four physically-modelled resonators, binaural HRTF spatialization, Euclidean percussion clustered across the whole field, a warm 3D spatial delay, a three-ring reactive oscilloscope, a kick that physically repels the field, and a mastering chain targeting −14 LUFS — one HTML instrument, packaged as a Kotlin WebView shell built entirely in GitHub Actions.

## Status — signed Android release built, installed, and owner-confirmed

| Phase | State |
|---|---|
| 0 — Planning | ✅ Done. Plan, ADRs, docs approved. |
| 1 — Minimum viable APK | ✅ Done. Confirmed on owner device after launch-crash regression was caught and fixed. |
| 2 — Native integration | ✅ Shipped: AndroidHost JS bridge, audio focus, `BACKGROUND AUDIO` foreground service, haptics on BOMB/FIRE, Stage Mode. The 10-minute screen-off soak and music-app focus check remain useful regression checks, but the app has been owner-confirmed functional. |
| 3 — Release engineering | ✅ Done for direct distribution: keystore generated, backed up off-device, GitHub secrets set, tag `v9.3.0` published, signed APK/AAB release workflow completed, and signed build installed successfully on owner device. |
| 4 — Native enhancements | Not started: native MIDI, native recorder, Oboe percussion — demand-driven, see `PLAN.md` §7. |

**Current public status:** signed Android release · device-confirmed · public-release ready for direct distribution.

Full gate-by-gate detail and session history: **[`docs/PHASE-CHECKLISTS.md`](docs/PHASE-CHECKLISTS.md)**.

## Get the signed release

The signed release is published at:

- https://github.com/renardoberou/Ping-thing-android/releases/tag/v9.3.0

Release assets include signed APK, AAB, and `CHECKSUMS.txt`. Install the APK for direct Android distribution. Upload the AAB only to Google Play when/if Play distribution is pursued.

Debug and signed release builds use different signatures. Android may require uninstalling the debug build before installing the signed build. Export presets first with the in-app **↓ SAVE / EXPORT** control if moving from a debug install.

## Known limitations

- **Web MIDI does not exist in Android WebView.** MIDI-learn and hardware MIDI controllers do not work in the Android app, though they can work in desktop browsers. A native `android.media.midi` bridge is scoped as Phase 4.
- **REC / MediaRecorder behavior inside WebView has not been explicitly verified** on-device. If it fails, a native recorder fallback is scoped in Phase 4.
- **No automated test suite.** Verification is currently CI build checks plus owner device testing.
- Portrait-locked; no tablet/landscape layout yet.

## Privacy

Privacy policy draft/page: [`privacy.html`](privacy.html).

Summary: the app does not require an account and does not intentionally collect personal data or telemetry. It uses internet permission for bundled-web compatibility/fonts, vibration for haptics, notification/foreground-service permissions for background audio, and local storage for presets/settings.

## Map

| File | What it is |
|---|---|
| [`PLAN.md`](PLAN.md) | Master build plan: architecture, phases, gates, risks |
| [`web/ping-thing.html`](web/ping-thing.html) | The instrument: single source of truth, also bundled into Android assets at build time |
| [`android/`](android/) | Kotlin WebView shell — `MainActivity`, bridge/focus/lifecycle/crash visibility, `PlaybackService` |
| [`docs/AGENT_GUIDE.md`](docs/AGENT_GUIDE.md) | How agents/contributors work here |
| [`docs/ADR-001-architecture.md`](docs/ADR-001-architecture.md) | WebView-first architecture rationale |
| [`docs/PHASE-CHECKLISTS.md`](docs/PHASE-CHECKLISTS.md) | Live gates and session log |
| [`docs/RELEASE.md`](docs/RELEASE.md) | Signing and release runbook |

## Build philosophy

Zero local tooling. Every APK or AAB this repo produces is built by GitHub Actions from a clean checkout, and installed straight from Actions/Releases onto a phone. No keystore, secret, `.env`, or binary build output is committed to this repository.

---
© Resonant Systems. All rights reserved.
