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
- [x] **Owner device matrix** — owner confirms app "opens and works perfectly" (2026-06-10); individual items not itemised; REC status unverified (logged, Phase 4 fallback specced)
- [x] **GATE: owner states "Phase 1 accepted"** *(owner: "I installed the app, it opens and works perfectly. Proceed with phase 2.", 2026-06-10)*
  - REGRESSION 2026-06-10: instant launch crash on owner device. Hotfix build adds
    on-screen crash capture (files/crash.txt rendered full-screen with COPY button),
    a guard around WebView construction (System-WebView-unavailable is a classic
    silent instant-crash), and init-order hygiene (setContentView before immersive).
    Phase 2 work stashed per owner instruction until this gate closes.

## Phase 2 — Native integration
- [x] JS bridge `AndroidHost` (feature-detected in HTML; additive `web:` commit)
- [x] Audio focus request/loss/regain wired to ctx.suspend/resume
- [x] Foreground service (mediaPlayback) behind in-app "BACKGROUND AUDIO" toggle, with STOP notification action
- [x] Haptic tap bridge for BOMB/FIRE
- [x] Stage Mode (radar-only fullscreen view for TV mirroring; works in browsers too)
- [x] **GATE: Phase 2** — closed by owner instruction "Initiate phase 3" (2026-06-10); soak/focus results not individually reported, items remain for regression reference

## Phase 3 — Release
- [ ] Keystore created via RELEASE.md phone-only path; secrets set; off-device backup confirmed by owner
- [x] `release.yml`: tag `v*` → signed APK + AAB attached to GitHub Release
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
- 2026-06-10 — consulting agent — Launch crash reported on owner device ("Ping Thing keeps stopping"); instrument HTML independently verified fine (Hermes/Termux), so fault is in the Kotlin shell. Phase 2 WIP stashed (git stash: "phase2-wip"). Shipped fix build: crash handler + full-screen trace viewer with COPY, WebView-construction guard, immersive-after-setContentView. Architecture question raised by owner answered in conversation: HTML is bundled in-APK (offline, no hosting); appassets.androidplatform.net is a virtual local origin, not the web. Next action: owner installs new artifact — either it launches clean, or it now SHOWS the stack trace; owner sends trace/screenshot back.
- 2026-06-10 — consulting agent — Phase 1 gate closed by owner. Phase 2 implemented: merged MainActivity (crash scaffolding kept + AndroidHost bridge + audio focus + conditional lifecycle), PlaybackService (mediaPlayback FGS, STOP action), manifest permissions, and sanctioned `web:` changes (bridge glue, BG AUDIO injected toggle, adaptive scheduler lookahead 2.5s/500ms when hidden, haptics, Stage Mode). Next action: owner installs new artifact and runs the Phase 2 gate — BG AUDIO on, screen off 10 min, audio must continue without drift; then focus check vs a music app.
- 2026-06-10 — consulting agent — Phase 2 gate closed by owner. Phase 3 shipped: env-driven release signing in app/build.gradle.kts (inert without secrets), release.yml (tag v* → decode KEYSTORE_B64 → assembleRelease+bundleRelease → assets on GitHub Release, versionName from tag). SECURITY: RELEASE.md Path B (CI keystore generation) retired — dispatch inputs and artifacts are publicly visible on a public repo; Path A (Termux) is canonical, Path C (agent-assisted secret setup, requires PAT Secrets RW) documented. Next action: owner runs Path A keytool commands, sets the four secrets (web UI or Path C), confirms off-device keystore backup, then creates tag v9.3.0 — release.yml does the rest.
