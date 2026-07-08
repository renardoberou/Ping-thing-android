# PHASE CHECKLISTS — gates are hard

## Phase 0 — Bootstrap
- [x] Repo created with PLAN.md, docs/, web/ping-thing.html
- [x] **GATE: owner approves PLAN.md and green-lights Phase 1** *(owner: "All phases approved", 2026-06-10)*

## Phase 1 — Minimum Viable APK
- [x] `android/` Gradle project (Kotlin, single Activity, applicationId `com.resonantsystems.pingthing`, minSdk 26, targetSdk 35)
- [x] WebViewAssetLoader serving `assets/ping-thing.html` under `https://appassets.androidplatform.net/`
- [x] Gradle `copyInstrument` task: `web/ping-thing.html` → assets at build time
- [x] WebView config: JS on, DOM storage on, `mediaPlaybackRequiresUserGesture=false`, zoom off
- [x] Immersive fullscreen, keep-screen-on, cutout shortEdges, portrait lock, double-back-to-exit
- [x] Lifecycle suspend/resume of AudioContext
- [x] Adaptive icon
- [x] `.github/workflows/build.yml` → debug APK artifact on push
- [x] **Owner device matrix** — owner confirms app opens and works on device; REC status remains explicitly unverified
- [x] **GATE: owner states "Phase 1 accepted"** *(2026-06-10)*

## Phase 2 — Native integration
- [x] JS bridge `AndroidHost`
- [x] Audio focus request/loss/regain wired to ctx.suspend/resume
- [x] Foreground service behind in-app "BACKGROUND AUDIO" toggle, with STOP notification action
- [x] Haptic tap bridge for BOMB/FIRE
- [x] Stage Mode
- [x] **GATE: Phase 2** — closed by owner instruction (2026-06-10); soak/focus results remain useful regression checks

## Phase 3 — Release
- [x] Keystore created and backed up off-device
- [x] GitHub release workflow configured for signed APK + AAB on `v*` tags
- [ ] Privacy policy page live via GitHub Pages / public URL — draft now exists as `privacy.html`; enable/link for Play Store when needed
- [x] `v9.3.0` tagged; release artifacts published — https://github.com/renardoberou/Ping-thing-android/releases/tag/v9.3.0
- [x] **GATE: signed build installed & verified on owner device** — owner reports signed APK installed and app works as expected (2026-07-08)

## Phase 4 — Backlog
- [ ] Native MIDI bridge
- [ ] Native recorder if REC fails
- [ ] Oboe percussion layer if latency requires it
- [ ] Play Store listing

---

## Session log

- 2026-06-10 — Phase 0 completed. Plan and docs approved.
- 2026-06-10 — Phase 1 implemented: Android shell, WebViewAssetLoader, pure-XML adaptive icon, copyInstrument asset pipeline, build workflow, XML validation.
- 2026-06-10 — Launch crash reported and fixed with on-screen crash visibility, WebView-construction guard, and initialization-order cleanup.
- 2026-06-10 — Phase 2 implemented: AndroidHost bridge, audio focus, foreground playback service, BG AUDIO toggle, adaptive scheduler, haptics, and Stage Mode.
- 2026-06-10 — Phase 3 release infrastructure added: signing-ready Gradle config and tag-triggered release workflow.
- 2026-07-08 — Audit/sync pass confirmed Phases 0–3 code was already present on `main`; README was updated from outdated Phase 0 wording to actual status.
- 2026-07-08 — Release hardening added: APK verification, checksums, safety scan, and release documentation improvements.
- 2026-07-08 — Signed release created for tag `v9.3.0`; release assets published with checksums.
- 2026-07-08 — Owner reports keystore backed up off-device, signed APK installed, and signed app works as expected. Phase 3 direct-distribution gate is closed. Remaining: public privacy URL for Play Store, optional REC verification, optional background-audio/focus regression checks, and Play Store listing decision.
