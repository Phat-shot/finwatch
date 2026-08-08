# Changelog

All notable changes to Finwatch are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning is a plain `1.N` release counter driven by CI, not semver — see
"Versioning" in the README. The `1.0`–`1.3` entries below were written
retroactively and summarize the release-preparation work thematically; they
don't map 1:1 onto individual CI build numbers. History before that (the
app's earlier life as "jellywear", with releases named after raw CI run
numbers such as "jellywear v173") is not itemized here.

## [Unreleased]

## [1.11] - 2026-08-06

### Added
- "Sign in with password instead" on the Quick Connect screen: servers with
  Quick Connect enabled previously offered no way to reach the
  username/password flow, which made them unusable when confirming the code
  from another device isn't practical (e.g. the public Jellyfin demo server).
- Explicit consent step in the login flow before connecting over cleartext
  `http://` (shown when the https-first probe falls back to http or an
  `http://` URL is entered explicitly): password and media would travel
  unencrypted, so connecting now requires a deliberate "Connect anyway".
- Localized, watch-sized error messages (network / authentication / server /
  generic) on the login, library, and player screens instead of the SDK's
  raw English exception text, in all five app languages.
- `CHANGELOG.md` (this file) and `CONTRIBUTING.md`.

### Changed
- Auto Backup and device-to-device transfer now exclude the session prefs
  file (encrypted access token, server URL, username). The Keystore key it
  is encrypted with never leaves the device, so a restored token could never
  be decrypted anyway — after a restore the app now starts with a clean
  sign-in instead of a half-restored session. App settings (theme, colors,
  playback, libraries) remain backed up.

### Fixed
- A server answering with a web page instead of the API — what happens when
  the base path is missing from the server URL, e.g. `demo.jellyfin.org`
  instead of `demo.jellyfin.org/stable` — reported a misleading generic
  failure. It now says the address carries no Jellyfin API.

## [1.3] - 2026-07-31

### Added
- Wear OS Ongoing Activity: while playback runs, the media notification is
  marked ongoing and a chip on the watch face jumps straight back into the
  now-playing screen.
- TalkBack (accessibility) labels for icon-only player controls, selection
  checkmarks, and playback cover art.

### Fixed
- Ongoing-activity notification channel info is provided, and the media
  notification is kept ongoing while playing so the watch-face chip shows
  reliably.

## [1.2] - 2026-07-30

### Added
- R8 code minification and resource shrinking for release builds, with keep
  rules for jellyfin-sdk/kotlinx.serialization and slf4j.
- First unit tests: server-URL probing, time formatting, category routing.
- User-facing `1.N` version scheme layered on top of the CI run-number
  `versionCode` (documented in the README).
- Release documentation: privacy policy (EN/DE), Play store listing texts,
  Play Console answers, tester checklist.

### Changed
- New-install defaults: Jellyfin-blue accent color; the Audiobooks and
  Playlists tiles start hidden (re-enable in Settings > Libraries).

## [1.1] - 2026-07-30

### Changed
- App renamed to **Finwatch** (previously "jellywear"); `app_name` marked
  non-translatable. The `one.srz.jellywear` code namespace is kept.
- Toolchain upgrade: Gradle 8.14.5, AGP 8.13.2, Kotlin 2.3.21;
  compileSdk/targetSdk raised 34 → 36 (Play target-API requirement).
- Dependency upgrades: Compose BOM 2026.06.01, Wear Compose 1.6.2, Media3
  1.4.1 → 1.10.1, jellyfin-sdk 1.8.6 → 1.8.12, Coil 2.7.0, slf4j 2.0.18.

### Fixed
- Lint `UnsafeOptInUsageError` (`@UnstableApi` no longer propagated from
  `PlaybackService`) and `LocalContext`-as-Activity cast replaced with
  `LocalActivity`.

## [1.0] - 2026-07-30

First release under the Finwatch release process.

### Added
- MPL-2.0 license and third-party notices.
- Two-stage CI: a validate job (lint + unit tests) gating build/release;
  secrets-based release signing with debug fallback; AAB build and Play
  upload workflow.

### Security
- Schemeless server URLs are probed https-first, with http only as a
  fallback.
- The login retry only ever upgrades http → https, never downgrades, so
  credentials are never silently resent over cleartext.
- Passwords are passed through unmodified (no more trimming/mangling in the
  login flow).
- The access token was moved out of stream/image URLs (query parameter) into
  the `Authorization` header, keeping it out of server/proxy logs and cache
  keys.
- The Jellyfin SDK's HTTP request logging is quieted in release builds.

### Fixed
- The single-child folder auto-drill is bounded to 15 levels.
