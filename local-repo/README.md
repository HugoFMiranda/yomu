# Vendored dependencies

JitPack can no longer build these artifacts fresh: each one's own build
script depends on JCenter (`jcenter.bintray.com`), which was shut down
permanently in 2022. Any *new* JitPack build request for these fails at
their `classpath` resolution step before it even gets to compiling the
library itself — this is not transient, retrying does not help.

These are prebuilt copies of already-successfully-resolved versions,
pinned exactly to what `app/build.gradle.kts` requires:

- `com.github.florent37:viewtooltip:1.2.2`
- `br.com.simplepass:loading-button-android:2.2.0`
- `com.dmitrymalkovich.android:material-design-dimens:1.4`
- `com.github.inorichi.injekt:injekt-core:65b0440`

Wired in as a repository in the root `build.gradle.kts`. No action needed
to use them — Gradle checks this folder before falling back to JitPack.
