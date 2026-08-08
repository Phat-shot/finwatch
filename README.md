# Finwatch

An **unofficial** Jellyfin client for Wear OS — browse your libraries and
play audio or video, built with Kotlin + Jetpack Compose for Wear OS +
Media3. Not affiliated with or endorsed by the Jellyfin project.

## Install

Finwatch requires a Wear OS watch and your own, reachable
[Jellyfin](https://jellyfin.org) server — the app is only a client.

- **GitHub Releases** —
  [github.com/Phat-shot/finwatch/releases](https://github.com/Phat-shot/finwatch/releases):
  stable releases are tagged `v1.N` with the APK attached as
  `finwatch-v1.N.apk`; beta builds from the `test` branch are
  pre-releases tagged `v1.N-test` (`finwatch-v1.N-test.apk`, separate
  app id `one.srz.finwatch.beta`). Sideload onto the watch via ADB
  (enable developer options + wireless debugging on the watch, then
  `adb install finwatch-v1.N.apk`).
- **[Obtainium](https://github.com/ImranR98/Obtainium)** — to get
  update notifications for sideloaded installs, add the app in
  Obtainium with the repo URL `https://github.com/Phat-shot/finwatch`.
  Obtainium then tracks new GitHub releases automatically (enable
  "Include prereleases" only if you want the beta channel).
- **Coming soon** — Google Play (closed beta first, testers welcome —
  see the issue tracker) and, once accepted, the
  [IzzyOnDroid](https://apt.izzysoft.de/fdroid/) F-Droid repo.

**Signing note:** APKs from GitHub Releases (and later IzzyOnDroid) are
signed with the developer's release key (certificate SHA-256 starting
`99:4D:DA:1D`). The future Google Play version will be re-signed by Play
App Signing and therefore carries a *different* signature — Android will
refuse to update one over the other. Pick one install source per watch;
switching later means uninstall + reinstall.

## License

Finwatch is free software, licensed under the
[Mozilla Public License 2.0](LICENSE). Licenses of bundled third-party
components (including the LGPL-3.0-licensed
[jellyfin-sdk-kotlin](https://github.com/jellyfin/jellyfin-sdk-kotlin))
are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Contributing & changelog

Pull requests go against the `test` branch — see
[CONTRIBUTING.md](CONTRIBUTING.md) for the branch model, build
prerequisites, and PR expectations. Notable changes are tracked in
[CHANGELOG.md](CHANGELOG.md); every user-visible PR adds an entry to its
`[Unreleased]` section.

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
| `main` | `prod` | Finwatch       | `one.srz.finwatch`       | `finwatch-v1.N.apk`      |
| `test` | `beta` | Finwatch beta  | `one.srz.finwatch.beta`  | `finwatch-v1.N-test.apk` |

`1.N` is the user-facing version — see "Versioning" below.

## Versioning

Two counters, one source:

- **`versionCode`** (what Play and devices compare) is the GitHub Actions
  run number of the shared `build.yml` workflow, passed to Gradle as
  `-PappVersionCode`. Pushes to `test` and `main` draw from the *same*
  counter, so every CI build — beta or prod — gets a strictly increasing
  code.
- **`versionName`** (what users see) is `1.N` with
  `N = versionCode − versionNameOffset`, clamped at 0. It is a plain
  release counter, not semver: `1.9 → 1.10 → 1.11`. Git tags and release
  names follow it (`v1.N`, `v1.N-test`, "Finwatch v1.N").

`versionNameOffset` is defined **once**, in `gradle.properties`. Gradle
reads it from there, and both workflows do too (`build.yml` greps it to
build tag and release names) — there is no second copy to keep in sync. It
was set right before the first `main` release so that release came out as
`v1.0` (offset = that run's number, 70); it must never change now, or `N`
would jump or regress.

**Builds without `-PappVersionCode`** — a plain checkout, notably
F-Droid building a tag from source — derive the version from the nearest
`v1.N` release tag instead (`N + versionNameOffset`), so a tagged build
carries the same version as the corresponding CI release. Beta tags
(`v1.N-test`) and the pre-rename `v<run>` tags are ignored. Only a
checkout with no such tag in its history at all falls back to
`versionCode 1`.

**Play uploads:** `play-publish.yml` asks for an explicit `versionCode`.
Always enter the run number of the `main` build being published (visible
in that run's logs and equal to its GitHub release's build). That way the
Play artifact is version-identical to the GitHub release, and both Play
and GitHub versions stay synchronized and strictly increasing. Re-runs of
a workflow keep their original run number — never re-upload a re-run to
Play; push a new commit instead.

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
   artifact.

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
./gradlew assembleProdRelease   # Finwatch
./gradlew assembleBetaRelease   # Finwatch beta (run scripts/badge_launcher_icon.py first for the badged icon)
```

Requires JDK 17 and the Android SDK (compileSdk 36). The build uses
Gradle 8.14 (wrapper) with AGP 8.13 and Kotlin 2.3.

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
- The toolchain is on the last AGP 8.x/Gradle 8.x line by design; the
  AGP 9 + Gradle 9 + Kotlin 2.4 (and possibly Coil 3) migration is a
  separate follow-up (see issue #21). Playback lifecycle around
  pause/task-removal should be re-tested on a watch after the Media3
  1.4 -> 1.10 jump.
- No CHANGELOG or CONTRIBUTING yet; release notes only exist as GitHub
  Release entries.
- No privacy policy URL yet — Google Play requires one for every app.
