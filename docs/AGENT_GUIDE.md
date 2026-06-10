# AGENT GUIDE — ping-thing-android

You are an AI agent (or human) picking up this project. Read this fully before acting.

## What this is
A native Android packaging of **The Ping Thing** (Resonant Systems) — a browser-based generative spatial sequencer. The instrument itself is a single, mature HTML file. Your job is the Android shell and release pipeline, **not** the instrument.

## The one rule that matters most
**`web/ping-thing.html` is the instrument and the owner's primary artifact.** Months of audio-engineering decisions live in it (gain staging, resonator physics, compressor chain, scheduler). Never modify it as a side effect of Android work. If an Android requirement seems to need an HTML change (e.g., the JS bridge feature-detect in Phase 2), make it a separate, clearly-labelled commit, additive and feature-detected, so the file still runs identically in a plain browser.

## Owner constraints (hard)
- The owner works **from an Android phone only**: GitHub web UI, GitHub Actions, Termux. No laptop, no Android Studio, no adb.
- Therefore: everything builds in CI; every artifact is downloadable from the Actions/Releases page; every credential procedure has a phone-only path (see `docs/RELEASE.md`).
- The owner tests by installing the artifact APK on their device and reporting back.

## Process
1. **Phase gates are hard.** Current phase status lives in `docs/PHASE-CHECKLISTS.md`. Do not start phase N+1 work before phase N is checked off and owner-confirmed.
2. **One PR per phase deliverable.** Small, reviewable, with a plain-language description the owner can read on a phone screen.
3. **Never commit secrets, keystores, or generated assets** (`android/app/src/main/assets/` is build-generated and git-ignored).
4. **CI must stay green on `main`.** If you break the build, fixing it precedes all other work.
5. Commit style: `phase1: add WebView shell`, `ci: cache gradle`, `docs: …`, `web: …` (the last only for sanctioned instrument changes).

## Technical landmarks you must know
- WebView serving via **WebViewAssetLoader** (`https://appassets.androidplatform.net/…`), never `file://` — localStorage origin stability depends on it (presets!).
- `mediaPlaybackRequiresUserGesture = false`; the HTML contains its own iOS/Android audio-unlock code — do not duplicate it natively.
- The HTML already handles safe-areas, viewport, touch, and keyboard-dismissal. The shell should be thin.
- Web MIDI does **not** exist in WebView. Known, accepted for v1, Phase 4 bridges it natively. Don't burn time "fixing" it earlier.
- The scheduler is JS-timer-driven with Web Audio lookahead: background throttling is real. Phase 2's foreground service is the designed answer.

## Where things are
- Master plan & phase specs: `PLAN.md`
- Architecture rationale: `docs/ADR-001-architecture.md`
- Release/signing runbook (phone-only): `docs/RELEASE.md`
- Gates: `docs/PHASE-CHECKLISTS.md`

## How to hand off
Before ending a session: push all work, ensure CI is green, update the relevant checklist, and leave a short `## Session log` entry at the bottom of `docs/PHASE-CHECKLISTS.md` stating what you did, what's verified, and the exact next action.
