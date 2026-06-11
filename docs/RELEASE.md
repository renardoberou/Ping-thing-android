# RELEASE.md — signing & release runbook (no computer required)

> **The keystore is the crown jewel.** Lose it and no future update can ever be installed over a released build (and a Play listing would be permanently orphaned). Back it up before the first release. Two copies minimum, one off-device.

## 1. Create the keystore — Path A: Termux (preferred)

```bash
pkg update && pkg install openjdk-17
keytool -genkeypair -v \
  -keystore pingthing-release.jks \
  -alias pingthing \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Resonant Systems, O=Resonant Systems"
# choose a strong store password; use the SAME password for the key when asked (simplifies CI)
base64 -w0 pingthing-release.jks > keystore.b64
```
Then in the GitHub repo → Settings → Secrets and variables → Actions, create:
- `KEYSTORE_B64` — contents of `keystore.b64`
- `KEYSTORE_PASS` — the store password
- `KEY_ALIAS` — `pingthing`
- `KEY_PASS` — the key password (same as store if you followed above)

Back up `pingthing-release.jks` (NOT the .b64 in cleartext anywhere public): private cloud drive + one additional location. Delete `keystore.b64` after pasting.

## 2. ~~Path B: one-shot CI workflow~~ — RETIRED (security)

The originally-planned CI keystore generator is **unsafe on a public repository**
and must not be built: `workflow_dispatch` inputs are visible on the run page to
anyone, and artifacts on public repos are downloadable by any logged-in user
while retention lasts. A signing keystore must never transit either channel.
Path A (Termux) is the supported phone-only procedure.

## 2b. Path C: agent-assisted secret setup (optional convenience)

After generating the keystore in Termux (Path A), instead of typing four secrets
into the GitHub web UI you may hand them to the build agent in conversation and
have it set them via the API. Requirements: the fine-grained PAT needs
**Secrets: Read and write** on this repository. Trade-off to understand: the
keystore base64 and passwords then exist in the conversation transcript — for a
self-published instrument app this is usually acceptable, but it is your call.
Path A + manual web entry remains the most private option.

## 3. Releasing
1. Confirm CI green on `main`.
2. Bump `versionName` if needed (semver mirrors instrument version, e.g. `9.3.0`).
3. Create tag `v9.3.0` via GitHub web UI (Releases → Draft new release → new tag on main).
4. `release.yml` triggers: decodes `KEYSTORE_B64`, runs `assembleRelease` + `bundleRelease`, attaches `pingthing-v9.3.0.apk` and `.aab` to the Release.
5. Install the APK from the Release page on the owner device; run a 5-minute sanity set.

`versionCode` is `github.run_number` — strictly increasing, no manual bookkeeping.

## 4. Upgrade-path note (debug → release)
Debug and release builds have different signatures: Android treats them as different apps for data purposes, so installing release over a long-used debug build requires uninstall → presets would be lost. **Before first release:** export the preset bank (in-app ↓ EXPORT) and re-import after. From the first signed release onward, all updates preserve data.

## 5. Play Store (when/if owner decides)
- Privacy policy: static page in this repo via GitHub Pages ("The Ping Thing stores all data locally on your device and transmits nothing." — expand to the standard 5 short sections).
- Content rating questionnaire: trivial (no UGC, no ads, no data).
- Assets needed: 2–8 phone screenshots (captured on device), 1024×500 feature graphic (can be generated from brand assets in a CI ImageMagick job), 512×512 icon (already produced in Phase 1).
- Target API: 35 already satisfies current policy.
