# ADR-001 — WebView-first native shell

**Status:** Accepted (Phase 0)
**Decision owner:** B. (Renardoberou) with consulting agent

## Context
The Ping Thing is ~3,300 lines of production Web Audio + canvas code: four physically-modelled IIR resonator types, per-voice HRTF spatialization, synthesized spring-reverb convolution, a warm spatial feedback delay, Euclidean percussion generation, a parallel-compressed master chain targeting −14 LUFS, and a dual-clock sample-accurate scheduler. It is a finished instrument with a paying-product roadmap.

The owner builds exclusively from an Android phone via GitHub web + Actions (precedent: ClaudeOS, an Android launcher APK built 100% in CI).

## Decision
Package the instrument in a **Kotlin single-Activity WebView shell**, served through `WebViewAssetLoader` under a secure synthetic origin, built and signed entirely in GitHub Actions. Deepen native integration in phases (audio focus → foreground service → JS bridge → optional native MIDI / Oboe percussion), never rewriting the engine.

## Rationale
1. **Engine preservation is the dominant requirement.** Every alternative that rewrites DSP (Kotlin/Oboe, Flutter) multiplies risk on a finished, tuned audio engine for marginal v1 benefit.
2. **Android WebView is Chromium:** full Web Audio (AAudio-backed on 8.1+), GPU canvas, AudioWorklet, localStorage. Measured Web Audio output latency on modern devices in WebView is in the 20–40 ms class — acceptable for this instrument; the only latency-critical gesture (BOMB tap) already fires audio on `touchstart`.
3. **CI-only buildability** is proven for plain Gradle/Kotlin Android projects (ClaudeOS). Capacitor adds a Node toolchain for zero functional gain here.
4. **Incremental escape hatch:** the JS-bridge architecture lets us move *individual* latency-critical voices (kick/hats) to Oboe later without touching the resonator engine — decision deferred until real-device measurements justify it (Phase 4 trigger: BOMB tap latency > ~35 ms).

## Consequences
- **Accepted losses in v1:** Web MIDI (WebView lacks it → Phase 4 native bridge); background playback until Phase 2.
- **Must-verify:** MediaRecorder (REC button) inside WebView — explicit Phase 1 test item with a specced native fallback.
- The HTML remains runnable in any browser unchanged; Android-specific code paths are feature-detected (`window.AndroidHost`).
- App identity `com.resonantsystems.pingthing` is committed and immutable post-release.

## Alternatives rejected
- **Capacitor/Cordova** — same WebView, heavier pipeline.
- **Full native rewrite** — months, high regression risk, violates engine-preservation.
- **Flutter/KMP** — total rewrite, no reuse.
- **TWA (Trusted Web Activity)** — requires a hosted HTTPS origin + digital asset links; couples the app to web hosting and Chrome; offline-first asset packaging is cleaner for a paid instrument.
