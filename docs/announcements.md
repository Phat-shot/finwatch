# Ankündigungstexte (Entwürfe — noch NICHT veröffentlicht)

Stand: 2026-10-15 (App ist seit v1.33 in Google Play live). Vier fertige
Texte (Englisch). Vor dem Posten Platzhalter prüfen. Refs #18.

Reihenfolge-Empfehlung: erst (b) awesome-jellyfin (dauerhafte Sichtbarkeit,
kein Timing nötig), dann (a) Jellyfin-Forum, dann mit ein paar Tagen
Abstand (c) r/selfhosted und (d) r/WearOS — nie alles am selben Tag,
und nach jedem Post 1–2 Tage Kommentare beantworten.

Verifizierter Stand der Kanäle:

- **forum.jellyfin.org**: Third-Party-Client-Vorstellungen laufen im
  Forum **"Client Development"** (Sektion "Development",
  <https://forum.jellyfin.org/f-client-development>) — dort liegen z. B.
  die Ankündigungen von Switchfin und MuseVu. Forum-Regeln:
  <https://forum.jellyfin.org/t-jellyfin-forum-rules> (u. a. kein
  Piraterie-Bezug; ehrlich als eigenes Projekt kennzeichnen).
- **awesome-jellyfin**: Das Clients-Repo
  `github.com/awesome-jellyfin/clients` wurde in das Monorepo
  **`github.com/awesome-jellyfin/awesome-jellyfin`** überführt.
  Client-Einträge werden **nicht** in `CLIENTS.md` editiert (die Datei
  ist generiert), sondern in **`assets/clients/clients.yaml`**.
- **r/jellyfin**: seit Juni 2023 read-only (Team ist zu
  forum.jellyfin.org umgezogen; Stand Juli 2026 unverändert). Alternative
  für Reddit: **r/selfhosted** (Regeln/Flair vor dem Posten im Sidebar
  prüfen).

---

## (a) Jellyfin-Forum — forum.jellyfin.org, Forum "Client Development"

**Titel:**

> Finwatch — unofficial Jellyfin client for Wear OS (music, audiobooks, video on your watch)

**Text:**

Hi everyone,

I'd like to share **Finwatch**, an unofficial, open-source Jellyfin
client for Wear OS I've been building. It's a standalone watch app — no
phone needed nearby — for browsing your Jellyfin libraries and playing
audio and video straight from your own server on the watch.

**Features**

- Sign in via Quick Connect (short code — no password typing on the
  watch) or with server URL / username / password; the access token is
  stored encrypted via the Android Keystore
- Compact home screen with configurable categories: Music, Audiobooks,
  Series, Movies, Favorites, Playlists
- Browse libraries, artists/albums and folders, with shuffle play on
  every screen
- Audio/video playback via ExoPlayer (Media3): background playback with
  system media controls, auto-recovery after short network drops
- Output to Bluetooth headphones or the watch speaker; optional
  server-side transcoding for video (audio always direct-plays)
- Always-dark UI with accent + font color pickers, five languages (EN,
  DE, FR, ES, AR)
- No tracking, no analytics, no ads

**Links**

- Source (MPL-2.0): https://github.com/Phat-shot/finwatch
- Releases (APK, sideload/Obtainium):
  https://github.com/Phat-shot/finwatch/releases
- Google Play: https://play.google.com/store/apps/details?id=one.srz.finwatch

The easiest install is the Play Store directly on the watch (search
"Finwatch") or "Install on more devices" from the web listing; the
GitHub APKs stay available for sideloading. Note that the Play build is signed by Play App Signing and
the GitHub APKs by my own key, so pick one source per watch; switching
means uninstall + reinstall.

To be clear: Finwatch is a third-party project — not affiliated with or
endorsed by the Jellyfin project. You need your own Jellyfin server; the
app provides no content. Feedback, bug reports and PRs are very welcome!

---

## (b) awesome-jellyfin — PR gegen `awesome-jellyfin/awesome-jellyfin`

Vorgehen: Fork von
<https://github.com/awesome-jellyfin/awesome-jellyfin>, Eintrag in
**`assets/clients/clients.yaml`** ergänzen (NICHT `CLIENTS.md` — die ist
aus der YAML generiert), CONTRIBUTING.md des Repos beachten, PR stellen.

Vorgeschlagener YAML-Eintrag (Feldnamen entsprechen bestehenden
Einträgen wie Finamp/Findroid):

```yaml
- name: "Finwatch"
  targets: [Android]
  types: [Music]
  oss: https://github.com/Phat-shot/finwatch
  official: false
  beta: false
  downloads:
    - type: github
      url: https://github.com/Phat-shot/finwatch/releases
```

Hinweise:

- In der Target-Liste des Repos existiert **kein Wear-OS-/Wearable-Target**
  (nur Browser/Desktop/Mobile/TV mit u. a. Android, iOS …). Optionen: im
  PR ein neues Target `WearOS` vorschlagen oder vorerst unter `Android`
  einsortieren und "Wear OS" im PR-Text erwähnen. [ZU PRÜFEN: exakte
  Syntax des `github`-Download-Typs mit einem bestehenden Eintrag
  und des Play-Typs mit einem bestehenden Eintrag (z. B. Finamp)
  abgleichen.]
- `types: [Music]` ist optional (Badge "🎵 Music Client"); Finwatch
  spielt auch Video — ggf. weglassen, wenn das Badge irreführend wirkt.

**PR-Titel:**

> Add Finwatch (unofficial Jellyfin client for Wear OS)

**PR-Text:**

This PR adds **Finwatch**, an unofficial, open-source (MPL-2.0) Jellyfin
client for **Wear OS** — a standalone smartwatch app for browsing
Jellyfin libraries and playing music, audiobooks and video directly from
the user's server, with background playback, Quick Connect sign-in and
no tracking/analytics.

- Source: https://github.com/Phat-shot/finwatch
- Releases (APK): https://github.com/Phat-shot/finwatch/releases
- Not affiliated with the Jellyfin project (clearly labeled unofficial,
  follows the Jellyfin third-party branding policy)

There is currently no Wear OS target in the list, so I've filed it under
Android — happy to adjust (or add a dedicated WearOS target) if you
prefer.

---

## (c) Reddit — r/selfhosted (Alternative zu r/jellyfin, das read-only ist)

Vor dem Posten: Subreddit-Regeln/Flair live prüfen (Self-Promotion ist
in r/selfhosted üblich, aber gekennzeichnet und sparsam). Flair z. B.
"Release".

**Titel:**

> Finwatch — an open-source Jellyfin client for Wear OS (standalone: browse + play from your own server, right on the watch)

**Text:**

Since r/jellyfin has been read-only for a while, posting here: I've
released **Finwatch**, an unofficial, open-source (MPL-2.0) Jellyfin
client for Wear OS.

It's fully standalone — the watch talks directly to your Jellyfin
server, no phone needed: browse music, audiobooks, shows and movies,
play audio/video with background playback and system media controls,
sign in via Quick Connect, output to Bluetooth headphones or the watch
speaker. No tracking, no ads; you obviously need your own Jellyfin
server.

On Google Play: https://play.google.com/store/apps/details?id=one.srz.finwatch
Source + APKs (sideload or Obtainium):
https://github.com/Phat-shot/finwatch — not affiliated with the
Jellyfin project. Feedback and bug reports very welcome!

---

## (d) Reddit — r/WearOS (Wear-OS-Nutzer, nicht nur Selfhoster)

Vor dem Posten: Subreddit-Regeln zu Self-Promotion/App-Vorstellungen
prüfen; Flair "App" o. ä. Zielgruppe hat eine Wear-OS-Uhr, kennt
Jellyfin aber oft nicht — deshalb ein Satz mehr dazu, was Jellyfin ist.
Einen Screenshot (Player-Screen) als Bildpost anhängen zieht deutlich
mehr als ein reiner Textpost.

**Titel:**

> Finwatch: play music, audiobooks and video from your own Jellyfin server on your watch (open source, on Google Play)

**Text:**

If you run a Jellyfin server (the free, self-hosted media server) and
have a Wear OS 3+ watch: Finwatch is a standalone watch client I built
— no phone needed, the watch talks to your server directly. Browse
music/audiobooks/shows/movies, background playback with system media
controls, Bluetooth headphones or the watch speaker, Quick Connect
sign-in so you don't type a password on the watch. Open source
(MPL-2.0), no tracking, no ads.

Play Store: https://play.google.com/store/apps/details?id=one.srz.finwatch
Source/APKs: https://github.com/Phat-shot/finwatch

You need your own Jellyfin server — the app has no content of its own.
Unofficial project, not affiliated with Jellyfin. Happy to hear what
breaks on your watch model!
