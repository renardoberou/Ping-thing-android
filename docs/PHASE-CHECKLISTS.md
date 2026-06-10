# PHASE CHECKLISTS — gates are hard

## Phase 0 — Bootstrap
- [x] Repo created with PLAN.md, docs/, web/ping-thing.html
- [ ] **GATE: owner approves PLAN.md and green-lights Phase 1**

## Phase 1 — Minimum Viable APK
- [ ] `android/` Gradle project (Kotlin, single Activity, applicationId `com.resonantsystems.pingthing`, minSdk 26, targetSdk 35)
- [ ] WebViewAssetLoader serving `assets/ping-thing.html` under `https://appassets.androidplatform.net/`
- [ ] Gradle `copyInstrument` task: `web/ping-thing.html` → assets at build time (assets git-ignored)
- [ ] WebView config: JS on, DOM storage on, `mediaPlaybackRequiresUserGesture=false`, zoom off
- [ ] Immersive fullscreen, keep-screen-on, cutout shortEdges, portrait lock, double-back-to-exit
- [ ] Lifecycle suspend/resume of AudioContext
- [ ] Adaptive icon (generated once in CI, committed)
- [ ] `.github/workflows/build.yml` → debug APK artifact on push
- [ ] **Owner device matrix (PLAN §4.5, 9 items) all pass** — REC item may be waived with logged issue
- [ ] **GATE: owner states "Phase 1 accepted"**

## Phase 2 — Native integration
- [ ] JS bridge `AndroidHost` (feature-detected in HTML; additive `web:` commit)
- [ ] Audio focus request/loss/regain wired to ctx.suspend/resume
- [ ] Foreground service (mediaPlayback) behind in-app "BACKGROUND AUDIO" toggle, with notification controls
- [ ] Haptic tap bridge for BOMB/FIRE
- [ ] Stage Mode (radar-only fullscreen view for TV mirroring)
- [ ] **GATE: 10-min screen-off soak, no scheduler drift; focus check vs a music app**

## Phase 3 — Release
- [ ] Keystore created via RELEASE.md phone-only path; secrets set; off-device backup confirmed by owner
- [ ] `release.yml`: tag `v*` → signed APK + AAB attached to GitHub Release
- [ ] Privacy policy page (GitHub Pages) live
- [ ] v9.3.0 tagged; release artifacts install & run
- [ ] **GATE: signed build verified on owner device; preset-migration note published**

## Phase 4 — Backlog (demand-driven, post-release)
- [ ] Native MIDI bridge (android.media.midi → handleMIDI)
- [ ] Native recorder (only if Phase 1 REC failed)
- [ ] Oboe percussion layer (only if BOMB tap latency measured > ~35 ms)
- [ ] Play Store listing (owner decision)

---

## Session log
*(append entries here on handoff: date — agent — what was done — what is verified — exact next action)*

- 2026-06-10 — consulting agent — Phase 0 documents authored; repo content staged for creation. Next action: owner creates repo / provides PAT; then owner reviews PLAN.md and closes Phase 0 gate.
