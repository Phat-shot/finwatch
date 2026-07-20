# jellywear

A Jellyfin client for Wear OS — audio library playback and a video demo
screen, built with Kotlin + Jetpack Compose for Wear OS + Media3.

This repo currently holds the project scaffold (buildable skeleton app +
CI) that the actual Jellyfin integration (login, audio library browsing,
playback, video demo) will be built on top of.

## Branches

- `test` — default branch, all development happens here.
- `main` — release branch. Merge `test` into `main` to cut a release.

## Build variants

Two product flavors share one codebase, selected by which branch triggers
CI:

| Branch | Flavor | App label      | Application ID          | Release asset            |
|--------|--------|----------------|--------------------------|---------------------------|
| `main` | `prod` | jellywear      | `one.srz.jellywear`      | `jellywear-v#.apk`        |
| `test` | `beta` | jellywear beta | `one.srz.jellywear.beta` | `jellywear-v#-test.apk`   |

`#` is the GitHub Actions run number, passed to Gradle as `-PappVersionCode`.

The beta variant additionally gets a "BETA" ribbon stamped onto the
launcher icon at CI time (`scripts/badge_launcher_icon.py`), so it's
visually distinguishable from the main app when both are installed on the
same watch. The badge is generated fresh on every CI run — nothing beta-
icon-related is committed to the repo.

## CI

`.github/workflows/build.yml` builds the matching flavor's release APK on
every push to `main` or `test`, uploads it as a workflow artifact, and
publishes it as a GitHub Release (`main` → normal release, `test` →
pre-release).

Both variants are currently signed with the Gradle debug signing config so
CI can produce an installable APK without any secrets set up yet. Swap in
a real keystore (via repo secrets) in `app/build.gradle.kts` before
shipping a public release.

## Local build

```bash
./gradlew assembleProdRelease   # jellywear
./gradlew assembleBetaRelease   # jellywear beta (run scripts/badge_launcher_icon.py first for the badged icon)
```

Requires JDK 17 and the Android SDK (compileSdk 34).

## Regenerating the base launcher icon

```bash
python3 scripts/generate_launcher_icons.py
```

## Roadmap

- Jellyfin server login / connection management
- Audio library browsing and playback (Media3/ExoPlayer)
- Video `/demo/` playback screen
- Release signing via a real keystore
