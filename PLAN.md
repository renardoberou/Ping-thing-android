# THE PING THING — Native Android Build Plan

**Repository:** `Renardoberou/ping-thing-android`
**Product:** The Ping Thing, by Resonant Systems — generative spatial sequencer
**Goal:** A standalone, installable, signed native Android app (APK + AAB), built **entirely via GitHub Actions CI** with zero local tooling, preserving 100% of the existing instrument.
**Status:** PLANNING — no application code exists yet. This document is the contract for all future work.

---

## 0. Ground rules (read before doing anything)

1. **No phase begins until the previous phase's checklist is merged and the owner (B.) has confirmed on a real device.** Phase gates are hard gates.
2. **The instrument source of truth is `web/ping-thing.html`.** All instrument changes happen there. The Android layer *embeds* it; it never forks it.
3. **The audio engine is protected.** No agent modifies signal-chain values (resonator coefficients, compressor settings, gain staging, EQ defaults) as a side effect of Android work. Instrument changes are a separate workstream with separate commits.
4. **Owner constraint:** B. develops exclusively from an Android phone via the GitHub web interface and GitHub Actions. Every procedure in this plan must be executable without a computer. Termux is available as a fallback shell.
5. **Builds must be reproducible from a clean clone by CI alone.** If a step requires a local machine, the step is wrong.

---

## 1. Architecture decision (summary — full ADR in `docs/ADR-001-architecture.md`)

**Chosen: Kotlin WebView shell, phased toward deeper native integration.**

The instrument is ~3,300 lines of mature Web Audio code: IIR resonator banks, HRTF panners, convolver reverb, a feedback delay, a parallel-compression master chain, a dual-clock sample-accurate scheduler, and a canvas renderer. Android System WebView (Chromium) executes all of it natively-backed: Web Audio on Android 8.1+ rides on AAudio, canvas is GPU-accelerated.

| Option | Verdict |
|---|---|
| **A. Kotlin + WebView (chosen)** | 100% engine reuse, ships in days, proven by the ClaudeOS CI pattern, incremental native upgrades possible |
| B. Capacitor/Cordova | Same WebView underneath plus a node toolchain we don't need; rejected for build complexity |
| C. Full native rewrite (Kotlin/Oboe) | Months of high-risk DSP porting for ~20ms latency gain; rejected for v1, revisit only if Phase 4 measurements demand it |
| D. Flutter/KMP | Total rewrite, no engine reuse; rejected |

Known WebView trade-offs, all handled in-plan:
- **Web MIDI is unavailable in WebView** (Chrome-only API) → Phase 4 native bridge via `android.media.midi`.
- **JS timers throttle when backgrounded** → Phase 2 foreground service + audio focus.
- **MediaRecorder support must be verified on-device** → Phase 1 test item; native fallback specced in Phase 4.

---

## 2. Repository layout (target state)

```
ping-thing-android/
├── README.md                  # front page
├── PLAN.md                    # this document
├── docs/
│   ├── ADR-001-architecture.md
│   ├── AGENT_GUIDE.md         # how future agents work here
│   ├── PHASE-CHECKLISTS.md    # tick-box gates per phase
│   └── RELEASE.md             # signing & release runbook (no-computer procedures)
├── web/
│   └── ping-thing.html        # THE instrument. Single source of truth.
├── android/                   # created in Phase 1
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradle/wrapper/…
│   └── app/
│       ├── build.gradle.kts   # copies web/ping-thing.html into assets at build time
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── java/com/resonantsystems/pingthing/MainActivity.kt
│           ├── assets/        # populated by gradle task — never committed
│           └── res/           # adaptive icon, splash, strings, themes
└── .github/workflows/
    ├── build.yml              # debug APK on every push (artifact)
    ├── release.yml            # signed APK+AAB on tag v*
    └── generate-keystore.yml  # one-shot, manual, then deleted (see RELEASE.md)
```

**App identity (fixed now, immutable after first release):**
- `applicationId`: `com.resonantsystems.pingthing`
- `minSdk` 26 (Android 8.0 — AAudio era, ~97% of active devices)
- `targetSdk` 35
- `versionName` semver mirroring instrument version (start `9.3.0`); `versionCode` = CI `github.run_number`

---

## 3. Phase 0 — Repository bootstrap *(this phase, no app code)*

**Deliverables:** this repo containing `README.md`, `PLAN.md`, `docs/*`, `web/ping-thing.html`.
**Exit gate:** owner confirms plan; explicitly green-lights Phase 1.

---

## 4. Phase 1 — Minimum Viable APK

**Objective:** a debug APK downloadable from CI artifacts that runs the full instrument fullscreen on a real device, indistinguishable in behaviour from the mobile-browser version (minus Web MIDI).

### 4.1 Android shell
- Single `MainActivity` (Kotlin), no fragments, no nav.
- **`WebViewAssetLoader`** serving `https://appassets.androidplatform.net/assets/ping-thing.html`. This is mandatory, not `file://`: it gives a stable secure origin so `localStorage` (presets, tutorial-seen, kick/hat state) persists correctly across app updates.
- WebView settings: `javaScriptEnabled`, `domStorageEnabled`, **`mediaPlaybackRequiresUserGesture = false`** (the HTML's own unlock code then guarantees start), `setSupportZoom(false)`.
- Immersive sticky fullscreen; `FLAG_KEEP_SCREEN_ON` while resumed (performance instrument — screen must not sleep mid-set). Cutout mode `shortEdges`; the HTML's existing `viewport-fit=cover` + safe-area CSS already handles notches.
- Lifecycle: `onPause → evaluateJavascript("if(window.ctx)ctx.suspend()")`, `onResume → ctx.resume()`. Clean, no audio bleed when switched away (background audio is a Phase 2 *feature*, not a Phase 1 accident).
- Back button: confirm-to-exit (double-press), since back would otherwise kill a live set.
- Orientation: locked portrait for v1 (matches the 480px design column).

### 4.2 Asset pipeline
Gradle task `copyInstrument` (registered before `preBuild`): copies `../../web/ping-thing.html` → `app/src/main/assets/`. Assets dir is git-ignored. One source of truth, enforced by the build.

### 4.3 CI — `build.yml`
```yaml
on: { push: { branches: [main] }, workflow_dispatch: {} }
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4   # temurin 17
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew assembleDebug
        working-directory: android
      - uses: actions/upload-artifact@v4   # app-debug.apk
```
Owner installs by: Actions → run → artifact → download on phone → install (Unknown Sources enabled once).

### 4.4 Icon & splash
Adaptive icon derived from the existing brand mark (dark field `#090807`, phosphor-green concentric rings, three enemy dots in gold/blue/green). PNGs for all densities generated **once** in a CI job with ImageMagick and committed — no design tooling needed.

### 4.5 Phase 1 device test matrix (owner, on phone)
1. Cold start → 5s boot glitch sequence plays → instrument loads.
2. SPAWN/SWARM/DEFEND/NUKE all behave; nuke shake works.
3. BOMB: presets 1–5, tap-overdub mode, repulsion + gravity boost visible.
4. FIRE: Euclidean hats follow enemy clusters; tap mode records; hats audibly pass through delay+reverb.
5. Knobs glitch-free while playing (the in-place delay/reverb update must hold in WebView).
6. Presets save → force-stop app → relaunch → presets persist (localStorage proof).
7. **REC button** → confirm `.webm` downloads (this is the MediaRecorder-in-WebView verification; if it fails, log it — native recorder is Phase 4, not a Phase 1 blocker).
8. SHARE copies URL; MIDI export downloads `ping-thing-sequence_N.mid`.
9. Headphone HRTF spatial check; 20-minute CPU/thermal soak with 12 enemies + SWARM + DEFEND.

**Exit gate:** all of the above pass (item 7 may be waived with a logged issue); owner says "Phase 1 accepted".

---

## 5. Phase 2 — Native integration (the app stops feeling like a wrapper)

1. **Audio focus:** `AudioFocusRequest(GAIN)` on first JS audio start (via JS→Kotlin bridge callback); on transient loss → `ctx.suspend()`; on regain → `ctx.resume()`; on permanent loss → suspend + notification-safe state.
2. **Foreground service (`mediaPlayback` type) — optional toggle "BACKGROUND AUDIO":** keeps the WebView process unthrottled with screen off / app backgrounded, with a persistent notification (play/pause action wired through the JS bridge). This is the fix for JS-timer throttling killing the scheduler.
3. **JS bridge v1** (`@JavascriptInterface`, namespaced `AndroidHost`): `onAudioStarted()`, `keepAwake(bool)`, `hapticTap(ms)` (BOMB/FIRE haptics), `getAppVersion()`. The HTML feature-detects `window.AndroidHost` — the same file keeps working in plain browsers untouched.
4. **Hardware volume keys** → media stream (default in WebView activity, verify).
5. **Stage Mode** (carried over from product backlog): JS-side button hiding all chrome except the radar — pairs with Smart View/Miracast mirroring for the TV use case.

**Exit gate:** background-audio toggle survives 10 min screen-off without scheduler drift; focus handling verified against a music app.

---

## 6. Phase 3 — Release engineering

1. **Keystore, no-computer procedure** (full runbook in `docs/RELEASE.md`):
   - *Path A (preferred, Termux):* `pkg install openjdk-17` → `keytool -genkeypair …` → `base64 keystore.jks` → paste into repo secrets `KEYSTORE_B64`, `KEYSTORE_PASS`, `KEY_ALIAS`, `KEY_PASS`. Keystore file backed up to private storage + one off-device copy. **Loss of this file permanently orphans the Play listing.**
   - *Path B (CI one-shot):* manual `generate-keystore.yml` builds keystore, uploads as a **private artifact** (never echoed to logs), owner downloads on phone, extracts base64 in Termux, sets secrets, then the workflow file is deleted in the same PR that confirms secrets exist.
2. **`release.yml`** on tag `v*`: decode keystore → `assembleRelease` + `bundleRelease` → attach APK + AAB to a GitHub Release. Version name from tag; versionCode from run number.
3. **Distribution v1: GitHub Releases** (immediate). **Play Store track:** requires privacy-policy URL (static page via GitHub Pages from this repo — app collects nothing, policy is three sentences), content rating questionnaire, 2–8 screenshots (captured on-device), feature graphic. Listed as paid or free-with-Gumroad-link consistent with current Resonant Systems strategy — owner's call at gate.

**Exit gate:** signed release APK installs over debug cleanly; localStorage survives the upgrade path (debug→release will differ in signature — document that users migrate presets via the existing JSON export; from first release onward, updates are seamless).

---

## 7. Phase 4 — Native enhancements (backlog, strictly post-release)

| Item | Approach | Trigger |
|---|---|---|
| **Native MIDI** | `android.media.midi` → bridge CC/notes into the existing `handleMIDI(msg)` as synthetic events | First user request / owner need; restores MIDI-learn parity lost from Chrome |
| **Native recorder** | `AudioPlaybackCapture` or tee via bridge → WAV | Only if WebView MediaRecorder failed in Phase 1 |
| **Oboe percussion layer** | Kick/hats triggered over the bridge into an Oboe stream for <10ms tap latency; resonator engine stays in Web Audio | Only if measured BOMB tap latency on target devices exceeds ~35ms |
| **In-app updater** | Check GitHub Releases API, prompt | Post-Play-Store decision |

---

## 8. Risk register

| Risk | Severity | Mitigation |
|---|---|---|
| Web MIDI absent in WebView | Medium | Documented loss in v1; Phase 4 native bridge; instrument fully playable without it |
| JS throttling kills scheduler in background | High (for background use) | Phase 1 ships foreground-only honestly; Phase 2 foreground service |
| MediaRecorder unsupported/broken in some WebViews | Medium | Phase 1 explicit test; Phase 4 native fallback; export-MIDI unaffected |
| Keystore loss (no-computer workflow) | Critical | Dual-path runbook, mandatory off-device backup, secrets checklist in RELEASE.md |
| Old/disabled System WebView on user devices | Low-Med | minSdk 26 + runtime WebView version check → friendly "update Android System WebView" screen |
| HRTF CPU on low-end devices | Low | Existing LQ mode; soak test in Phase 1 matrix |
| localStorage loss across reinstall | Low | Existing preset JSON export is the documented backup path |

---

## 9. Sequencing summary

```
Phase 0  ──► owner approves plan          (this commit)
Phase 1  ──► debug APK in CI artifacts    (~1–2 working sessions)
   gate: 9-point device matrix
Phase 2  ──► focus + foreground service   (~2–3 sessions)
   gate: background soak test
Phase 3  ──► signed v9.3.0 GitHub Release (~1–2 sessions)
   gate: upgrade-path check
Phase 4  ──► backlog, demand-driven
```

*Estimates are working sessions of the established kind, not calendar days.*

---

## 10. First action for the next agent

Read `docs/AGENT_GUIDE.md`. Confirm Phase 0 gate is closed (owner approval in an issue or conversation). Then implement **Phase 1 exactly as §4 specifies**, in one PR: `android/` project + `build.yml` + icon job. Nothing else. Do not touch `web/ping-thing.html`.
