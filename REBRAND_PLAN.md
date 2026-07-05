# Rebrand Plan: TachiyomiJ2K → [YOUR APP NAME]

Spec-driven plan for forking this app under your own brand, with your own
package id, signing, and release pipeline. Written so a fresh agent session
can pick this up with no prior context.

## Goals

- App ships under your own name/icon, not "TachiyomiJ2K".
- You control signing, releases, and the update-check pipeline end to end.
- No dependency on the original maintainer's Firebase project or GitHub repo.
- Extensions continue to work unmodified (verified: `ExtensionLoader.kt` has
  no hardcoded package-name check, matches extensions by intent-filter
  action, not caller package).

## Decisions needed before starting (fill these in)

| Placeholder | Meaning | Decision |
|---|---|---|
| `{APP_NAME}` | Display name shown to users | `Yomu` |
| `{APPLICATION_ID}` | New Android package id | `com.hugofm.yomu` |
| `{URL_SCHEME}` | Deep-link scheme prefix | `yomu` |
| `{GITHUB_REPO}` | Your fork's `owner/repo` on GitHub | `HugoFMiranda/yomu` |
| `{KEEP_UPSTREAM_LINKS}` | Keep original Discord/GitHub links in About screen, or replace/remove? | remove |

---

## Spec 1: App identity (name, icon, applicationId)

**Requirement**: App displays as `{APP_NAME}` with a custom icon, installs as
`{APPLICATION_ID}`, independent from the original `eu.kanade.tachiyomi`.

**Files**:
- `app/src/main/res/values/strings.xml:3` — `app_name` string.
- `app/src/main/res/mipmap-*/` — launcher icon, all densities (mdpi → xxxhdpi)
  + `mipmap-anydpi-v26` adaptive icon (foreground/background layers) +
  monochrome themed-icon variant (Android 13+ requirement, else icon looks
  broken in themed-icon mode).
- Notification small icon — separate drawable (must stay a simple white
  silhouette per Android notification icon guidelines, not the full-color
  launcher icon).
- `app/build.gradle.kts:36` — `applicationId = "eu.kanade.tachiyomi"` →
  `{APPLICATION_ID}`.
- `app/src/main/AndroidManifest.xml` — deep-link `android:scheme` values:
  line 153 (`tachiyomij2k`) and lines 70/168/183/198 (`tachiyomi`) → change
  to `{URL_SCHEME}` and `{URL_SCHEME}j2k`-equivalent, or collapse to one
  scheme. NOTE: `FileProvider` (`:216`) and Shizuku (`:270`) authorities
  already use `${applicationId}` templating — no manual edit needed there.

**Acceptance criteria**:
- [ ] App installs alongside (not over) any existing TachiyomiJ2K install —
      confirms applicationId is genuinely independent.
- [ ] Launcher icon renders correctly in normal, adaptive, and themed
      (Android 13+ Material You) icon modes.
- [ ] Notification icon shows a clean monochrome silhouette, not a colored
      square (common failure mode when reusing a full-color icon here).
- [ ] Any manga source / backup that used `tachiyomi://` deep links either
      still resolves (if scheme kept) or fails gracefully (if changed).

**Note**: changing `applicationId` makes this a *new* app from Android's
perspective. Anyone with the old TachiyomiJ2K installed does **not** get
this as an update — they'd need to install fresh and migrate their library
via backup/restore (in-app feature, unaffected by this change).

---

## Spec 2: Remove Firebase / Crashlytics

**Requirement**: No dependency on the original maintainer's Firebase
project. Decision: **remove entirely** (only usage is one crash-reporting
toggle — not worth standing up a new Firebase project for).

**Files**:
- `app/build.gradle.kts:11` — remove `id("com.google.firebase.crashlytics")`.
- `app/build.gradle.kts:170-173` — remove `firebase-bom`,
  `firebase-analytics-ktx`, `firebase-crashlytics-ktx` deps.
- `build.gradle.kts:35` — remove `classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.4")`.
- `app/src/standard/google-services.json` — delete file.
- `app/src/main/java/eu/kanade/tachiyomi/ui/setting/SettingsAdvancedController.kt`
  — remove the crash-reporting toggle/reference (only usage site in codebase).

**Acceptance criteria**:
- [ ] Project builds with zero references to `com.google.firebase` or
      `crashlytics` anywhere in `app/build.gradle.kts` / `build.gradle.kts`.
- [ ] Settings screen no longer shows a crash-reporting option (or shows a
      working local-only alternative if you choose to add one later — out
      of scope for this pass).
- [ ] `google-services.json` no longer present in the repo.

---

## Spec 3: Your own signing keystore

**Requirement**: Releases signed with a keystore you generate and control,
not the original maintainer's.

**Steps** (run once, outside the repo, keep artifacts private):

```bash
keytool -genkey -v -keystore release.keystore -alias yourAlias \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore > release.keystore.base64
```

Add to GitHub repo → Settings → Secrets and variables → Actions:

| Secret name | Value |
|---|---|
| `SIGNING_KEY` | contents of `release.keystore.base64` |
| `ALIAS` | `yourAlias` |
| `KEY_STORE_PASSWORD` | keystore password |
| `KEY_PASSWORD` | key password |

No code change needed — `.github/workflows/build_push.yml:65-71` already
reads these exact secret names via `r0adkll/sign-android-release@v1`.

**Acceptance criteria**:
- [ ] `release.keystore` file exists, is backed up somewhere durable
      (password manager / encrypted storage), and is **not** committed to
      git.
- [ ] All 4 secrets set on the GitHub repo that will run CI.
- [ ] A CI-built release APK installs and is signed by your keystore
      (`apksigner verify --print-certs` shows your cert, not upstream's).
- [ ] **Do not rotate this keystore after the first public release** — a
      new keystore means users can never update in place again.

---

## Spec 4: Own release/update pipeline

**Requirement**: In-app update checker and CI release process point at your
GitHub repo, not `Jays2Kings/tachiyomiJ2K`.

**Files**:
- `app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateNotifier.kt`
  and wherever the update-check API call is built — point at
  `{GITHUB_REPO}` releases API instead of upstream.
- `.github/workflows/build_push.yml` — hardcoded `tachiyomij2k` string
  appears ~15 times (artifact filenames, release title `TachiyomiJ2K
  ${{ env.VERSION_TAG }}` at line 103, apk names lines 78-121). Rename to
  match `{APP_NAME}`.

**Acceptance criteria**:
- [ ] Fresh install of the app, followed by a new tagged release on
      `{GITHUB_REPO}`, triggers the in-app "update available" prompt.
- [ ] Downloaded update APK installs cleanly over the previous version
      (same applicationId + same signing keystore as Spec 1 & 3 — if either
      drifts, Android will refuse the update install).
- [ ] Release artifacts on GitHub are named after `{APP_NAME}`, not
      `tachiyomij2k-*`.

---

## Spec 5: About screen / links cleanup

**Requirement**: Decide fate of upstream links per `{KEEP_UPSTREAM_LINKS}`.

**Files**:
- `app/src/main/java/eu/kanade/tachiyomi/ui/more/AboutController.kt`
- `app/src/main/java/eu/kanade/tachiyomi/ui/more/AboutLinksPreference.kt`

**Acceptance criteria**:
- [ ] About screen links (Discord, GitHub, website) point where you decided
      — either your own repo/community, or removed if none exists yet.
- [ ] No leftover strings referencing "TachiyomiJ2K" or "Jays2Kings" in
      user-facing text (search `strings.xml` and the two files above).

---

## Out of scope / explicitly not needed

- **Firebase replacement**: not standing up a new Firebase project. If crash
  reporting is wanted later, treat as a separate feature spec.
- **Extension loader changes**: none required, already package-agnostic.
- **License changes**: project is Apache-2.0. Rebranding is permitted; keep
  the `LICENSE` file as-is, don't claim original authorship of upstream
  code.

## Suggested execution order

1. Spec 1 (identity) — biggest surface area, do first so everything else is
   built/tested under the final name.
2. Spec 2 (Firebase removal) — independent, can happen in parallel with 1.
3. Spec 3 (keystore) — needed before any real CI release.
4. Spec 4 (release pipeline) — depends on 1 (name) and 3 (keystore).
5. Spec 5 (links) — cosmetic, do last.
