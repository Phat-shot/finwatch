# Test-Server für das Play-Review

Finwatch ist ohne Jellyfin-Server nicht nutzbar, deshalb verlangt Play im
Formular **App access** Zugangsdaten (siehe `play-console-answers.md`,
Abschnitt 5). Dieses Dokument sammelt, womit der Server befüllt wird.

Zwei harte Anforderungen: Die Medien müssen **eindeutig frei lizenziert**
sein, und sie dürfen **inhaltlich nicht anecken** — die App ist mit USK 0
eingestuft, und der Reviewer sieht genau das, was auf dem Server liegt.

## Video — Blender Open Movies (CC-BY)

Bezugsquellen: <https://download.blender.org/demo/movies/> und
<https://studio.blender.org/films/>

| Film | Lizenz | Länge | Anmerkung |
|---|---|---|---|
| Big Buck Bunny | CC-BY 3.0 | 10 min | erste Wahl, völlig harmlos |
| Caminandes: Llama Drama / Llamigos | CC-BY | 2–3 min | winzig, ideal als Zweitfilm |
| Cosmos Laundromat | CC-BY | 12 min | unbedenklich |
| Sintel | CC-BY 3.0 | 15 min | milde Fantasy-Gewalt |
| Tears of Steel | CC-BY 3.0 | 12 min | Sci-Fi-Action |

Für ein USK-0-Listing die letzten beiden weglassen.

## Audio — bevorzugt CC0

- **Open Goldberg Variations** (Kimiko Ishizaka, Bach) — **CC0**, also
  gemeinfrei ohne Namensnennungspflicht, gute Qualität, saubere Tags.
  Ebenso das Wohltemperierte Klavier desselben Projekts.
  <https://opengoldbergvariations.org>, auch im Internet Archive.
- **Musopen** (<https://musopen.org>) — gemeinfreie Klassik-Aufnahmen.
- **Free Music Archive**, **Jamendo**, **ccMixter** — CC-lizenziert,
  Lizenz **pro Track** prüfen, nicht pauschal.
- **Incompetech** (Kevin MacLeod) — CC-BY, sehr großer Katalog.

CC0 ist CC-BY vorzuziehen: keine Attribution, keine Diskussion.

## Verzeichnisstruktur

Jellyfins Namenskonventionen einhalten, sonst bleibt die Bibliothek ohne
Titel und Cover — und genau daran erkennt ein Reviewer eine halbfertige
App:

```
Movies/
  Big Buck Bunny (2008)/
    Big Buck Bunny (2008).mp4
  Caminandes - Llamigos (2016)/
    Caminandes - Llamigos (2016).mp4
Music/
  Kimiko Ishizaka/
    Open Goldberg Variations/
      01 - Aria.mp3
      ...
      folder.jpg
```

Zwei Filme und ein Album reichen. Mehr Material verlängert nur den Scan
und bringt für das Review nichts.

## Reviewer-Zugang

- Eigener Jellyfin-Benutzer (z. B. `playreview`), **keine Adminrechte**.
- Zugangsdaten plus Serveradresse ins Play-Formular "App access"
  eintragen, mit Hinweis: Server-URL inklusive Pfad eingeben, falls der
  Server unter einem Unterpfad läuft.
- Server über den gesamten Review-Zeitraum erreichbar lassen — auch für
  spätere Update-Reviews, Google prüft erneut.

## Attribution

CC-BY verlangt Namensnennung. Auf einem privaten Review-Server ist das
keine Veröffentlichung im Sinne der Lizenz, sauberer ist es trotzdem:
eine `ATTRIBUTION.txt` neben die Medien legen ("Big Buck Bunny — (CC) 
Blender Foundation, blender.org"). Bei CC0-Audio entfällt das.
