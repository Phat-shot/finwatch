# jellywear

A Jellyfin client for Wear OS — browse your libraries and play audio or
video, built with Kotlin + Jetpack Compose for Wear OS + Media3.

## Features

- **Sign in** to a Jellyfin server (server URL / username / password,
  entered via Wear OS's `RemoteInput` text entry — `presentation/login`).
  The access token is encrypted at rest with an Android Keystore-backed
  AES key (`data/SecureTokenStore.kt`), not stored in plaintext.
- **Compact launcher** (`presentation/home/HomeScreen.kt`): a two-per-row
  icon grid for the library categories — Music, Audiobooks, Series,
  Movies, Favorites, Playlists — plus Settings. Tap to browse, long-press
  to shuffle-play that category. Which categories are shown is
  configurable in Settings > Libraries.
- **Browse** libraries, artists/albums, and folders
  (`presentation/library`: `CategoryScreen`, `ArtistAlbumsScreen`,
  `ItemBrowserScreen`), with a shuffle-play chip on each screen.
- **Play** audio or video (`presentation/player/PlayerScreen.kt`,
  `playback/PlaybackService.kt`): ExoPlayer via a `MediaSessionService`,
  so playback keeps running with system media controls after the app
  leaves the foreground (screen off, navigating elsewhere on the watch).
  - Video gets a `PlayerView` surface with auto-hiding controls; audio
    gets play/pause/skip, a position/duration readout, and (optionally)
    full-screen cover art.
  - Tapping the media notification or the watch-face playback icon
    reopens straight into the now-playing screen instead of the home
    screen.
  - Auto-recovers from transient playback errors (e.g. a network blip)
    by retrying up to 3 times before giving up.
  - Swiping the app away stops playback and the notification outright;
    just backgrounding it (screen off, navigating away) keeps audio
    going.
- **Settings** (`presentation/settings`):
  - *Appearance* — theme mode (dark/light/system), accent and font
    color pickers, cover art display mode, and app language (English,
    German, French, Spanish, Arabic, or follow system).
  - *Playback* — server-side transcode toggle (video only; audio always
    direct-plays) and, on watches with a built-in speaker, an output
    toggle to route audio to it instead of a connected Bluetooth device.
  - *Libraries* — which category tiles appear on the home screen.

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
pre-release). Releases built from `main` are additionally mirrored to the
public `jellywear-release` repo, so anyone can grab the latest APK
without access to this private repo.

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

- No seek control — playback shows a position/duration progress bar but
  it isn't scrubbable yet.
- Release signing still uses the Gradle debug signing config (see
  "CI" above) — swap in a real keystore via repo secrets before a public
  release.
