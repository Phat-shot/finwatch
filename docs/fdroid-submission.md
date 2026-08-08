# F-Droid — zwei Wege, ein Ziel

Stand: 2026-08-06. Refs #19. „F-Droid" meint zwei verschiedene Dinge, mit
sehr unterschiedlichem Aufwand:

| | IzzyOnDroid | F-Droid-Hauptrepo |
|---|---|---|
| Was | F-Droid-kompatibles Zusatz-Repo, das Nutzer im F-Droid-Client hinzufügen | Das offizielle `f-droid.org`-Repo, in jedem Client vorinstalliert |
| Woher die APK | unsere GitHub-Releases, unverändert | F-Droid **baut selbst aus dem Quellcode** |
| Signatur | unser Release-Key (identisch mit GitHub) | F-Droid-Key → **andere Signatur**, kein Update über GitHub-Installationen hinweg (außer per Reproducible Build) |
| Aufwand | ein GitLab-Issue | Merge Request mit Build-Metadaten, muss auf deren Buildserver bauen |
| Dauer | Tage bis wenige Wochen | Wochen bis Monate |
| Status | **einreichbar, alles vorbereitet** | ein Blocker offen (siehe unten) |

Empfehlung: **erst IzzyOnDroid** (sofort möglich, erreicht die
F-Droid-Nutzerschaft schon weitgehend), Hauptrepo als zweiter Schritt.

---

## 1. IzzyOnDroid

Antragstext und Schrittfolge: `docs/izzyondroid-submission.md`. Alle
Voraussetzungen sind erfüllt (öffentliches Repo, MPL-2.0, echte
Release-Signatur, Fastlane-Metadaten mit Icon, Screenshots und
Changelogs). Einzige laufende Pflicht danach: `changelogs/<versionCode>.txt`
**vor** jedem Tag pflegen.

---

## 2. F-Droid-Hauptrepo

### Was schon passt

- MPL-2.0, öffentliches Repo, Quellcode vollständig.
- **Keine proprietären Abhängigkeiten**: kein GMS, kein Firebase, keine
  Tracker. Alles Maven-Central-FOSS (AndroidX/Compose/Media3 Apache-2.0,
  jellyfin-sdk LGPL-3.0, Coil Apache-2.0, slf4j MIT).
- Getaggte Releases (`v1.N`) mit Changelog, Fastlane-Metadaten im Repo —
  fdroidserver zieht Beschreibung, Icon und Screenshots automatisch daraus.
- Metadaten-Entwurf liegt fertig unter
  `docs/fdroid-metadata/one.srz.finwatch.yml`.

### Der offene Blocker: woher kommt die Versionsnummer?

`versionCode` stammt heute aus der GitHub-Actions-Run-Nummer
(`-PappVersionCode`), `versionName` wird daraus abgeleitet. F-Droid baut
aber einen nackten Checkout des Tags — ohne diese Property fällt der Build
auf `versionCode 1` / `1.1` zurück.

Zwei Auswege:

1. **Werte in den Build-Metadaten hart eintragen** (`gradleprops`, so im
   Entwurf). Funktioniert sofort, aber `AutoUpdateMode` muss aus bleiben,
   d. h. **jedes** Release braucht einen neuen Merge Request bei
   fdroiddata — dauerhafte Handarbeit, und bei uns erzeugt jeder
   `main`-Merge ein Release.
2. **Version aus dem Git-Tag ableiten** (empfohlen): Wenn
   `-PappVersionCode` fehlt, liest `build.gradle.kts` den nächstgelegenen
   Tag `v1.N` und rechnet `versionCode = N + Offset`. CI-Builds bleiben
   unverändert (die Property gewinnt weiterhin), lokale Builds bekommen
   endlich sinnvolle Nummern, und F-Droid kann auf `AutoUpdateMode:
   Version` laufen — neue Releases landen dann ohne Zutun im Repo. Dazu
   sollte `VERSION_NAME_OFFSET` aus den beiden Workflow-Dateien in die
   `gradle.properties` wandern (heute doppelt gepflegt, siehe README).

Solange das nicht entschieden ist, sollte der Merge Request warten —
Variante 1 einzureichen bedeutet, sich die Handarbeit dauerhaft
einzuhandeln.

### Einreichungsweg (wenn der Blocker weg ist)

1. GitLab-Account, Fork von <https://gitlab.com/fdroid/fdroiddata>.
2. `metadata/one.srz.finwatch.yml` aus `docs/fdroid-metadata/` übernehmen.
3. Lokal prüfen — am einfachsten im offiziellen Docker-Image
   (`registry.gitlab.com/fdroid/fdroidserver:buildserver`):
   - `fdroid readmeta` und `fdroid lint one.srz.finwatch` (Syntax/Stil)
   - `fdroid build -v -l one.srz.finwatch` (echter Build wie auf deren
     Server — das ist der Test, der zählt)
4. Merge Request stellen. Alternativ zuerst ein RFP-Issue unter
   <https://gitlab.com/fdroid/rfp/-/issues> („Requests For Packaging"),
   wenn man das Packaging Freiwilligen überlassen will — dauert aber
   deutlich länger als der eigene MR.
5. Auf Review-Kommentare reagieren; typische Rückfragen betreffen
   Anti-Features, mitgelieferte Binärdateien und Build-Determinismus.

### Zu erwartende Rückfragen

- **`keystore/debug.keystore` im Repo**: eine Binärdatei im Quellbaum. Der
  Scanner schlägt darauf normalerweise nicht an, notfalls `scanignore`
  ergänzen oder die Datei aus dem Repo nehmen (sie existiert nur als
  Signatur-Fallback für Forks ohne Secrets).
- **Signatur-Frage**: F-Droid signiert mit eigenem Key → Nutzer können
  nicht zwischen GitHub-/Izzy-Version und F-Droid-Version updaten. Lösbar
  über **Reproducible Builds**: F-Droid baut nach, vergleicht Byte für
  Byte mit unserer Release-APK und veröffentlicht dann **unsere** signierte
  Datei (`Binaries:`-Feld in den Metadaten). Erfordert einen
  deterministischen Build (feste AGP-/Gradle-/JDK-Version — haben wir) und
  ist der sauberste Endzustand, aber kein Startpunkt.
- **Wear-only-App**: F-Droid-Clients zeigen sie normal an; die
  Screenshots liegen zusätzlich unter `images/wearScreenshots/`.
