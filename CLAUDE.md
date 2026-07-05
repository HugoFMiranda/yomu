# CLAUDE.md

Guidance for Claude Code sessions working in this repo.

## What this is

Fork of TachiyomiJ2K (Android manga reader, Kotlin + Jetpack Compose).
Currently mid-rebrand — see `REBRAND_PLAN.md` for the full spec (new name,
icon, applicationId, signing, release pipeline, Firebase removal).

## Environment

- Android SDK: `/var/www/tachiomi/android-sdk` (not the default
  `~/Android/sdk`). Export before any gradle/adb command:
  ```bash
  export ANDROID_SDK_ROOT=/var/www/tachiomi/android-sdk
  export ANDROID_HOME=/var/www/tachiomi/android-sdk
  ```
- This is a shared VPS (also runs unrelated production apps: next-server
  instances, php-fpm, artisan queue workers). **3.7GB RAM total, no
  dedicated headroom.** Don't assume a dev-laptop-sized box.

## Build memory constraints (read before building)

This box repeatedly OOMs on `assembleDebug` (all flavors) because:
- The default `assembleDebug` target builds `dev` + `standard` flavors in
  parallel, each spawning its own Kotlin compile daemon (~1.3GB RSS each).
  Two of those alone exceed available RAM.
- `kapt` spawns a *separate* Kotlin daemon from the main compile task even
  with `kotlin.compiler.execution.strategy=in-process` set — that flag only
  covers the main compile, not kapt stub generation.
- Compose compiler's IR lowering pass (`DeepCopyIrTreeWithSymbols`) is
  heap-hungry on this codebase; 2048M was not enough even for a single
  flavor, single-worker build. Bumped to `org.gradle.jvmargs=-Xmx3072M` in
  `gradle.properties` and set `kapt.use.worker.api=false` to consolidate
  kapt into the same JVM instead of spawning its own daemon.

**Recommended build command on this box**:
```bash
export ANDROID_SDK_ROOT=/var/www/tachiomi/android-sdk
export ANDROID_HOME=/var/www/tachiomi/android-sdk
./gradlew assembleStandardDebug --no-daemon --max-workers=1 \
  -Dkotlin.compiler.execution.strategy=in-process --console=plain
```
Single flavor (`assembleStandardDebug`, not `assembleDebug`), single
worker, no parallel flavor builds. Even so this box's compile could still
OOM depending on what else is running — check `free -h` and `ps aux
--sort=-%mem` first; kill stray `KotlinCompileDaemon`/`K2JVMCompiler`
processes left over from prior failed attempts before retrying (they don't
die on their own for up to 2h idle timeout and will stack with a new
attempt).

If it's still tight: this may genuinely need building on a bigger machine
(local dev machine or CI) rather than this VPS — don't burn excessive time
tuning JVM flags past this point, hardware is the ceiling, not config.

## Known-fixed issue: OkHttp pre-release version

App was pinned to `okhttp3` `5.0.0-alpha.11` (see `app/build.gradle.kts`),
an unstable pre-release whose internal ABI differs from stable releases.
This broke dynamically-loaded extension APKs (classloader mismatch on
OkHttp internals) — symptom was crashes on extension install/load.

Fix (see git history, branch `fix/okhttp-stable-version`): bumped to
`5.3.2` (stable) + removed alpha-only internal API usage
(`okhttp3.internal.toImmutableList()`) in `MangaUpdates.kt`, replaced with
plain `.toList()`. Also needed `-Xskip-metadata-version-check` kotlin
compiler flag once the newer okhttp's kotlin-stdlib metadata outpaced this
project's kotlinc version.

## Extensions

Extensions are separate APKs loaded at runtime.
`app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt`
matches them via intent-filter action, not hardcoded package name — safe
to change `applicationId` without breaking extension compatibility.

## Rebrand in progress

Full plan, file-by-file, in `REBRAND_PLAN.md`. Read it before doing any
branding/identity/signing/CI work — it has exact line numbers, acceptance
criteria per change, and flags what NOT to do (e.g. don't stand up a new
Firebase project, just remove it — only one usage site in the whole
codebase).
