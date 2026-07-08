# RELEASE.md — signing & release runbook (no computer required)

> **The keystore is the crown jewel.** Lose it and no future update can ever be installed over a released build (and a Play listing would be permanently orphaned). Back it up before the first release. Two copies minimum, one off-device.

## 0. Pre-flight safety check (run before every tag)

Every push to `main` already runs an automated `safety-scan` job (`.github/workflows/build.yml`) that fails the build if it finds token-shaped strings or a tracked `.jks`/`.apk`/`.aab`. Before cutting a release tag specifically, it's worth running the same check yourself on a fresh clone, since a bad tag is harder to walk back than a bad commit:

```bash
git clone --depth 1 https://github.com/renardoberou/Ping-thing-android.git && cd Ping-thing-android
git grep -n -i -E 'api[_-]?key|secret|token|password|ghp_|github_pat_' HEAD || echo "clean"
git grep -n -E '\.(jks|keystore|apk|aab)$' HEAD || echo "clean"
```
Expected: only this document's own placeholder names, never a real secret value or a tracked binary.

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

**Convenience alternative to the web UI:** if `gh` (GitHub CLI) is installed in Termux (`pkg install gh`, then `gh auth login` — supports a browser device-code flow, no token needed by hand), the same four secrets can be set from the same shell without leaving Termux:
```bash
gh secret set KEYSTORE_B64 --repo renardoberou/Ping-thing-android < keystore.b64
gh secret set KEYSTORE_PASS --repo renardoberou/Ping-thing-android
gh secret set KEY_ALIAS --repo renardoberou/Ping-thing-android --body "pingthing"
gh secret set KEY_PASS --repo renardoberou/Ping-thing-android
gh secret list --repo renardoberou/Ping-thing-android   # confirms names only, never values
```

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
4. `release.yml` triggers: decodes `KEYSTORE_B64`, runs `assembleRelease` + `bundleRelease`, verifies the resulting APK with `apksigner verify`, generates `CHECKSUMS.txt` (SHA-256 of both artifacts), and attaches all three files to the Release. If `apksigner` can't be located on the runner or verification fails, the workflow fails loudly rather than shipping an unverified build.
5. Install the APK from the Release page on the owner device; run a 5-minute sanity set. Optionally re-verify locally against `CHECKSUMS.txt` (`sha256sum -c CHECKSUMS.txt`) before installing on a second device.

`versionCode` is `github.run_number` — strictly increasing, no manual bookkeeping.

## 4. Upgrade-path note (debug → release)
Debug and release builds have different signatures: Android treats them as different apps for data purposes, so installing release over a long-used debug build requires uninstall → presets would be lost. **Before first release:** export the preset bank (in-app ↓ EXPORT) and re-import after. From the first signed release onward, all updates preserve data.

## 5. Play Store (when/if owner decides)
- **Signing key model:** the same private key generated in §1 can serve as the Play upload key (Play App Signing then re-signs for distribution automatically) — no separate keystore needed unless the owner later wants distinct upload-vs-distribution keys. See https://support.google.com/googleplay/android-developer/answer/9842756.
- **New personal developer accounts** are subject to a closed-testing gate before production access: **12 opted-in testers for 14 continuous days**. Plan the GitHub-Release APK phase to double as that test pool if going to Play. See https://support.google.com/googleplay/android-developer/answer/14151465.
- Privacy policy: static page in this repo via GitHub Pages ("The Ping Thing stores all data locally on your device and transmits nothing." — expand to the standard 5 short sections).
- Data safety form: only claim "no data collected" once actually verified true for the shipped build (WebView + localStorage only, no network calls beyond optional Google Fonts) — see §5a below.
- Content rating questionnaire: trivial (no UGC, no ads, no data).
- Assets needed: 2–8 phone screenshots (captured on device), 1024×500 feature graphic (can be generated from brand assets in a CI ImageMagick job), 512×512 icon (already produced in Phase 1).
- Target API: 35 already satisfies current Play policy (https://support.google.com/googleplay/android-developer/answer/11926878).
- Upload `.aab` (not `.apk`) to Play; the release workflow already produces both.

## 5a. Reference links
- Android app signing: https://developer.android.com/studio/publish/app-signing
- Android App Bundles: https://developer.android.com/guide/app-bundle
- `apksigner`: https://developer.android.com/studio/command-line/apksigner
- Android versioning: https://developer.android.com/studio/publish/versioning
- Play store listing assets: https://support.google.com/googleplay/android-developer/answer/1078870
