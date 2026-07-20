# jellywear

A Jellyfin client for Wear OS — audio library playback and video, built
with Kotlin + Jetpack Compose for Wear OS + Media3.

## Features

- Sign in to a Jellyfin server (server URL / username / password, entered
  via Wear OS's `RemoteInput` text entry — see `presentation/login`).
  The access token is encrypted at rest with an Android Keystore-backed
  AES key (`data/SecureTokenStore.kt`), not stored in plaintext.
- Browse your libraries and drill into folders (`presentation/library`).
- Play audio or video (`presentation/player`, `playback/PlaybackService.kt`):
  ExoPlayer via a `MediaSessionService`, so playback keeps running with
  system media controls after the app leaves the foreground (screen off,
  navigating elsewhere on the watch). Video gets a `PlayerView` surface,
  audio gets a play/pause control and a position/duration readout.

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

## Known gaps

- No seek control — playback position is shown but not scrubbable yet.
- Browsing is a generic folder view, not a music-tailored layout (no
  dedicated artist/album grid, no album art).
- Release signing still uses the Gradle debug signing config (see
  "CI" above) — swap in a real keystore via repo secrets before a public
  release.
