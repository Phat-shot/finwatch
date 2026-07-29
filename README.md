# jellywear

A Jellyfin client for Wear OS — browse your libraries and play audio or
video, built with Kotlin + Jetpack Compose for Wear OS + Media3.

## Features

- **Sign in** to a Jellyfin server (`presentation/login`): if the server
  has Quick Connect enabled, the watch shows a short code to confirm from
  another device — no password typing on the watch. Otherwise server URL /
  username / password are entered via Wear OS's `RemoteInput` text entry.
  Schemeless server URLs are probed https-first with an http fallback.
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

`.github/workflows/build.yml` runs in two stages:

1. **validate** — Android Lint + unit tests, on every push to `main`/`test`
   *and* on pull requests targeting them.
2. **build** (pushes only) — builds the matching flavor's release APK,
   uploads it as a workflow artifact, and publishes it as a GitHub Release
   (`main` → normal release, `test` → pre-release). `main` builds
   additionally produce the App Bundle (`bundleProdRelease`) that Google
   Play requires, attached to the release and uploaded as the `prod-aab`
   artifact. Releases built from `main` are also mirrored to the public
   `jellywear-release` repo (needs the `RELEASE_REPO_TOKEN` secret, a
   fine-grained PAT for that repo).

### Release signing

Signing is driven by repo secrets, with a debug fallback so forks and
secretless runs keep producing installable builds:

| Secret | Content |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | base64 of the release keystore file (`base64 -w0 release.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | keystore password |
| `RELEASE_KEY_ALIAS` | key alias |
| `RELEASE_KEY_PASSWORD` | key password |

When the secrets are absent, release builds are signed with the committed
debug keystore (`keystore/debug.keystore`) — fine for sideloading and the
beta channel, **not** accepted by Google Play. Locally the same values can
be supplied as Gradle properties (`releaseKeystoreFile`,
`releaseKeystorePassword`, `releaseKeyAlias`, `releaseKeyPassword`) in
`~/.gradle/gradle.properties`.

### Play Store upload

`.github/workflows/play-publish.yml` is a manually triggered workflow
(`workflow_dispatch`) that builds a prod AAB with an explicitly chosen
`versionCode` and uploads it to a selectable Play track via
`r0adkll/upload-google-play`. It additionally needs the
`PLAY_SERVICE_ACCOUNT_JSON` secret (JSON key of a service account linked
in Play Console → Setup → API access). Without the secrets the workflow
skips cleanly instead of failing.

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
- No real release keystore yet — the signing *mechanics* (secrets +
  debug fallback, see "Release signing" above) are in place, but the
  keystore itself still has to be generated and its secrets configured
  before a store release.
- `compileSdk`/`targetSdk` are 34. Google Play requires **targetSdk 35**
  for new Wear OS apps (existing ones by 2026-08-31), so an AGP +
  compileSdk upgrade is needed before a Play submission. Media3 is pinned
  to 1.4.1 for the same reason (1.5+ needs compileSdk 35).
- No CHANGELOG or CONTRIBUTING yet; release notes only exist as GitHub
  Release entries.
- No privacy policy URL yet — Google Play requires one for every app.
