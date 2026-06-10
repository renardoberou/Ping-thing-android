# The Ping Thing — Android

**Resonant Systems** · *Controlled systems. Unpredictable sound.*

Native Android packaging of **The Ping Thing**, a generative spatial sequencer: a radar field where enemies are voices, position is timbre and space, and a defender hunts the patterns you build. Four physically-modelled resonators, binaural HRTF spatialization, Euclidean percussion, a kick that physically repels the field, and a mastering chain targeting −14 LUFS — in one HTML file, here packaged as a signed, installable Android app built entirely in GitHub Actions.

## Status
**Phase 0 — Planning.** No application code yet, by design. Read **[PLAN.md](PLAN.md)** — the complete start-to-finish build plan and the contract for all work in this repo.

## Map
| File | What it is |
|---|---|
| [`PLAN.md`](PLAN.md) | Master build plan: architecture, phases, gates, risks |
| [`web/ping-thing.html`](web/ping-thing.html) | **The instrument.** Single source of truth (v9.3). Runs in any browser today |
| [`docs/AGENT_GUIDE.md`](docs/AGENT_GUIDE.md) | How agents/contributors work here — read before touching anything |
| [`docs/ADR-001-architecture.md`](docs/ADR-001-architecture.md) | Why WebView-first |
| [`docs/PHASE-CHECKLISTS.md`](docs/PHASE-CHECKLISTS.md) | Live gates + session log |
| [`docs/RELEASE.md`](docs/RELEASE.md) | Signing & release runbook (phone-only procedures) |

## Build philosophy
Zero local tooling. Every APK this repo ever produces is built, signed, and published by GitHub Actions, and installed straight from the Actions/Releases page onto a phone.

---
© Resonant Systems. All rights reserved.
