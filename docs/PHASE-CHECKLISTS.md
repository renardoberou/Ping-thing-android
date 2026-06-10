# PHASE CHECKLISTS — gates are hard

## Phase 0 — Bootstrap
- [x] Repo created with PLAN.md, docs/, web/ping-thing.html
- [x] **GATE: owner approves PLAN.md and green-lights Phase 1** *(owner: "All phases approved", 2026-06-10)*

## Phase 1 — Minimum Viable APK
- [x] `android/` Gradle project (Kotlin, single Activity, applicationId `com.resonantsystems.pingthing`, minSdk 26, targetSdk 35)
- [x] WebViewAssetLoader serving `assets/ping-thing.html` under `https://appassets.androidplatform.net/`
- [x] Gradle `copyInstrument` task: `web/ping-thing.html` → assets at build time (assets git-ignored)
- [x] WebView config: JS on, DOM storage on, `mediaPlaybackRequiresUserGesture=false`, zoom off
- [x] Immersive fullscreen, keep-screen-on, cutout shortEdges (values-v27), portrait lock, double-back-to-exit
- [x] Lifecycle suspend/resume of AudioContext
- [x] Adaptive icon (pure-XML vector + anydpi-v26 — minSdk 26 makes legacy PNGs unnecessary)
- [x] `.github/workflows/build.yml` → debug APK artifact on push
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
- 2026-06-10 — consulting agent — Phase 0 gate closed (owner approved all phases). Phase 1 implemented in full per PLAN §4: Android shell (Kotlin, plain Activity, WebViewAssetLoader, AGP 8.6.1 / Gradle 8.9 / Kotlin 2.0.20, sole dep androidx.webkit:1.11.0), pure-XML adaptive icon, copyInstrument asset pipeline, build.yml CI. All XML validated. **Verified next action: owner opens Actions tab → waits for green run → downloads `ping-thing-debug-apk` artifact → extracts zip → installs APK → runs the 9-point device matrix from PLAN §4.5 and reports results here.**
