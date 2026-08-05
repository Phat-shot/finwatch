# IzzyOnDroid-Aufnahmeantrag (Entwurf — noch NICHT eingereicht)

Stand: 2026-07-31. Dieses Dokument enthält den fertigen Antragstext
(Englisch) und die Schrittfolge für die Einreichung. Refs #19.

## Einreichungsweg (verifiziert)

Aufnahme wird als **Issue im GitLab-Repo `IzzyOnDroid/repo`** beantragt:

1. GitLab-Account nötig (gitlab.com).
2. Neues Issue anlegen: <https://gitlab.com/IzzyOnDroid/repo/-/issues/new>
   (Übersicht bestehender Anträge:
   <https://gitlab.com/IzzyOnDroid/repo/-/issues>; übliche Titelformate
   sind z. B. "New app: <Name>" oder "suggest_app: <Name>").
3. Vorher prüfen, dass die App weder im Repo
   (<https://apt.izzysoft.de/fdroid/>) noch im Issue-Tracker schon
   vorhanden ist, und bestätigen, dass die Inclusion-Kriterien erfüllt
   sind (README des Repos bzw.
   <https://izzyondroid.org/docs/general/AppInclusionPolicy/>).
4. Den Antragstext unten einfügen und absenden. Danach auf Rückfragen von
   Izzy im Issue reagieren.

Voraussetzungen, die vor dem Absenden erfüllt sein müssen:

- [ ] Repo öffentlich, LICENSE (MPL-2.0) vorhanden — ✓
- [ ] GitHub-Release **v1.0** mit `finwatch-v1.0.apk` ist veröffentlicht
      (Izzy holt die APKs aus GitHub Releases; Beta-Releases `v1.N-test`
      sind als Pre-Release markiert und sollen nicht gescannt werden)
- [ ] APK mit echtem Release-Key signiert, nicht `debuggable`/`testOnly` — ✓
      (SHA-256 des Zertifikats beginnt `99:4D:DA:1D`)
- [ ] Fastlane-Metadaten im Repo (`fastlane/metadata/android/en-US/` +
      `de-DE/`) inkl. `images/icon.png` und mindestens ein Screenshot
      unter `images/wearScreenshots/` [ZU PRÜFEN: Screenshots noch
      ergänzen — icon.png aus `docs/store-assets/icon-512.png` kopieren]

## Antragstext (Englisch, zum Einfügen ins GitLab-Issue)

Vorgeschlagener Issue-Titel:

> New app: Finwatch (unofficial Jellyfin client for Wear OS)

Issue-Text:

---

Hi Izzy,

I'd like to suggest my app **Finwatch** for inclusion in the IzzyOnDroid
repo.

**Finwatch** is an unofficial, open-source Jellyfin client for Wear OS
(standalone watch app, no phone required): browse your Jellyfin
libraries (music, audiobooks, shows, movies) and play audio/video
directly from your own server on the watch. No tracking, no analytics,
no ads.

- **Source repo:** https://github.com/Phat-shot/finwatch
- **License:** MPL-2.0 (LICENSE in the repo root). Bundled third-party
  components are listed in THIRD-PARTY-NOTICES.md; note that one
  dependency, [jellyfin-sdk-kotlin](https://github.com/jellyfin/jellyfin-sdk-kotlin),
  is LGPL-3.0 — all FOSS, no proprietary dependencies, no GMS/Firebase.
- **Releases (APKs):** https://github.com/Phat-shot/finwatch/releases —
  stable releases are tagged `v1.N` with the APK attached as
  `finwatch-v1.N.apk` (first stable release: tag `v1.0`,
  `finwatch-v1.0.apk`). Please ignore tags `v1.N-test`: those are beta
  builds from the `test` branch, marked as GitHub pre-releases, with a
  different applicationId (`one.srz.finwatch.beta`).
- **applicationId:** `one.srz.finwatch`
- **Signing:** signed with my own dedicated release key (consistent
  across all GitHub releases; certificate SHA-256 starts with
  `99:4D:DA:1D`). The APK is neither `debuggable` nor `testOnly`.
- **Size:** ~7 MB, well below the repo's per-app limit.
- **Fastlane metadata:** available in the repo under
  `fastlane/metadata/android/` (en-US and de-DE: title, short/full
  description, per-versionCode changelogs, icon).
- **Anti-Features:** none that I'm aware of. The app requires a
  self-hosted Jellyfin server instance to be useful (it is only a
  client and provides no content).

Finwatch is a third-party project, not affiliated with the Jellyfin
project; the app name, icon and store texts follow Jellyfin's
third-party branding policy (own name/logo, "Jellyfin" only used
descriptively, clearly labeled unofficial).

I confirm the app complies with the inclusion criteria and is not yet
listed in the repo or the issue tracker. Thanks for taking a look — and
for running the repo!

---

## Nach der Aufnahme

- README-Abschnitt "Install" um den IzzyOnDroid-Link ergänzen
  (`https://apt.izzysoft.de/fdroid/index/apk/one.srz.finwatch`) und ggf.
  Izzy-Badge einbauen.
- `changelogs/<versionCode>.txt` bei jedem Release **vor** dem Tag
  pflegen (Izzy liest die Metadaten zum Release-Stand; max. 500 Zeichen
  pro Datei).
- Optional später: Reproducible Builds anstreben (Izzy verifiziert und
  kennzeichnet die App dann entsprechend), F-Droid-Hauptrepo als
  separater Schritt (RFP-Issue bzw. Merge Request ans fdroiddata-Repo).
