# RELEASE.md — signing & release runbook

> **The keystore is the crown jewel.** Lose it and no future update can be installed over the released build. Keep at least two private backups, one off-device. Never commit the keystore, passwords, `.env` files, APKs, AABs, or logs.

## Current signed release status

- Release tag: `v9.3.0`
- Release URL: https://github.com/renardoberou/Ping-thing-android/releases/tag/v9.3.0
- Artifacts: signed APK, AAB, and `CHECKSUMS.txt`
- Verification: release workflow ran APK signing verification before publishing artifacts
- Owner status: keystore backed up off-device; signed APK installed on owner device; app works as expected

## Before every future release tag

Run a clean safety check on a fresh clone before tagging:

```bash
git clone --depth 1 https://github.com/renardoberou/Ping-thing-android.git
cd Ping-thing-android
git grep -n -i -E 'api[_-]?key|secret|token|password|ghp_|github_pat_' HEAD || echo "clean"
git grep -n -E '\.(jks|keystore|apk|aab)$' HEAD || echo "clean"
```

Expected: no real secret values and no tracked binary release artifacts.

## Keystore procedure

The release keystore is generated locally by the owner, backed up privately, and exposed to GitHub Actions only through repository secrets. Do not regenerate it for ordinary updates. Regenerating changes the app signature and can break update continuity.

Required repository secrets for release workflow:

- `KEYSTORE_B64`
- `KEYSTORE_PASS`
- `KEY_ALIAS`
- `KEY_PASS`

Do not write the secret values into this repo, issues, logs, README files, screenshots, or release notes.

## Releasing

1. Confirm CI green on `main`.
2. Bump `versionName` if needed.
3. Create a new `v*` tag on `main`.
4. `release.yml` builds signed APK + AAB, verifies the APK with `apksigner`, generates `CHECKSUMS.txt`, and attaches artifacts to the GitHub Release.
5. Install the APK from the Release page on the owner device and run a sanity set.
6. Update `docs/PHASE-CHECKLISTS.md` and README if release status changes.

`versionCode` is derived from the GitHub Actions run number and is strictly increasing.

## Upgrade-path note

Debug and release builds have different signatures. Android treats them as separate update paths. Before switching from a debug build to a signed release build, export the preset bank in-app and re-import after installing the signed build. From the first signed release onward, signed updates should preserve app data.

## Privacy policy

Draft/page in this repo: [`privacy.html`](../privacy.html).

Before Play Store submission, publish that page at a stable URL and add it to the store listing. Current privacy summary: the app does not require an account and does not intentionally collect or transmit personal data. It uses local storage for presets/settings, vibration for haptics, notifications/foreground service for background audio, and internet permission for bundled-web compatibility/fonts.

## Play Store notes

- Upload the `.aab`, not the `.apk`, to Play.
- Keep the same signing identity / Play App Signing strategy coherent across updates.
- New personal developer accounts may be subject to closed testing requirements before production access.
- Store assets still needed: phone screenshots, 1024×500 feature graphic, 512×512 icon, content rating, target audience declaration, data safety form.
