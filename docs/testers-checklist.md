# Closed-Testing-Checkliste — Finwatch

Für **persönliche** Play-Console-Konten, die nach dem 13.11.2023 erstellt
wurden, gilt: **mindestens 12 Tester, durchgehend über die letzten 14 Tage
opted-in**, bevor Produktionszugriff beantragt werden kann (ursprünglich 20,
seit 11.12.2024 auf 12 gesenkt; Organisationskonten sind ausgenommen).

## Schrittfolge in der Play Console

1. **App anlegen** (Name "Finwatch", App/Spiel, kostenlos) und alle
   "App content"-Formulare ausfüllen (siehe `play-console-answers.md`).
2. Optional: **Interner Test** (bis 100 Tester, sofort verfügbar) für einen
   ersten Smoke-Test des AAB.
3. **Testen → Geschlossener Test → Track erstellen** ("Closed testing"),
   signiertes **AAB** hochladen (Play App Signing einrichten), Länder
   auswählen, Release-Notes eintragen, Release ausrollen.
4. **Tester hinterlegen** (Tab "Tester" im Track):
   - **E-Mail-Liste**: Adressen einzeln pflegen; jede Adresse muss exakt dem
     Google-Konto des Testers entsprechen — fehleranfällig, aber ohne
     Zusatzdienst.
   - **Google-Group** (empfohlen): eine Gruppe (z. B.
     `finwatch-testers@googlegroups.com`) als Tester-Quelle eintragen; Tester
     treten der Gruppe selbst bei und sind damit automatisch berechtigt —
     weniger Pflegeaufwand bei 12+ Leuten.
5. **Opt-in-Link** (wird im Track angezeigt) an die Tester verteilen; Tester
   müssen dem Test **beitreten und die App aus Play installieren** (auf der
   Uhr: Play Store auf der Watch, gleicher Google-Account).
6. **Zähler beobachten**: Die Console zeigt an, wie viele Tester aktuell
   zählen. Wichtig: Die 14 Tage laufen nur, solange durchgehend ≥ 12 Tester
   opted-in sind — lieber 15–20 Tester einsammeln (Puffer für Abspringer).
7. Nach 14 Tagen: **"Produktionszugriff beantragen"** und die Fragen zum
   Testverlauf beantworten (wer getestet hat, was gefunden/gefixt wurde —
   ehrlich und konkret; Google bewertet das Engagement).
8. Danach Produktions-Release einreichen (inkl. Wear-OS-Quality-Review,
   Issue #15).

## Tester finden: Jellyfin-Community

**Wo posten:** Das offizielle **Jellyfin-Forum** (forum.jellyfin.org) — vorher
die dort angepinnten Forenregeln + Jellyfin Community Standards lesen und die
passende Kategorie wählen (Bereich für Drittanbieter-/Community-Projekte bzw.
Off-Topic; im Zweifel die Moderation fragen). Einmal posten, transparent als
eigenes Projekt kennzeichnen, nicht bumpen/crossposten. **Achtung:**
r/jellyfin ist seit Mitte 2023 read-only (Subreddit vom Team geschlossen) —
dort geht nichts mehr; Alternativen wie r/selfhosted haben eigene, strenge
Self-Promotion-Regeln (vorher lesen, oft nur an bestimmten Tagen/Threads
erlaubt).

**Textbaustein (EN) für den Aufruf:**

> **[Beta testers wanted] Finwatch — an unofficial open-source Jellyfin client
> for Wear OS**
>
> Hi all! I've been building Finwatch, a free and open-source (MPL-2.0)
> standalone Wear OS client for Jellyfin: browse your libraries and play
> music, audiobooks, shows and movies right from your watch — background
> playback, shuffle, optional transcoding, no phone required.
>
> To publish on Google Play, new personal developer accounts need a closed
> test with at least 12 testers opted in for 14 days — so I'm looking for a
> few people with a Wear OS 3+ watch and their own Jellyfin server who'd like
> to try it and share feedback. No server? The public demo works too: use
> `demo.jellyfin.org/stable` as the server URL, tap "Sign in with password
> instead", user `demo`, leave the password empty.
>
> How to join: send me your Google account email / join the Google Group
> [LINK], then install via the opt-in link [LINK]. Staying opted in for the
> full 14 days helps a lot, testing itself takes as much or as little time as
> you like.
>
> Source code & issues: https://github.com/Phat-shot/finwatch
> No tracking, no ads; your credentials only ever go to your own server.
> Finwatch is an unofficial client and not affiliated with the Jellyfin
> project. Mods: if this post fits better elsewhere or isn't welcome, please
> let me know and I'll adjust/remove it.
