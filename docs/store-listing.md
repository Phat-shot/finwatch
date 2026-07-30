# Play-Store-Listing — Finwatch (Entwurf)

Entwurf für das Store-Listing in Deutsch und Englisch. Limits laut Play Console:
**Titel max. 30 Zeichen, Kurzbeschreibung max. 80, Langbeschreibung max. 4000**
(Quelle: Play-Console-Hilfe "Best practices for your store listing").

**Jellyfin-Branding-Policy** (jellyfin.org/docs/general/contributing/branding):
Drittanbieter dürfen "Jellyfin" nicht als Projektnamen verwenden (auch
"Jelly[Wort]"- und "[Wort]fin"-Kombinationen sind unerwünscht — daher der Name
*Finwatch*, nicht *Jellywear*), das Jellyfin-Logo nicht als eigenes Logo nutzen
und sich nicht als offizielles Projekt ausgeben. Erlaubt ist der Hinweis auf
Kompatibilität im Untertitel/in der Beschreibung ("AppName, a Jellyfin
client"). Genau so ist das Listing formuliert: eigener Name + eigenes Logo,
"Jellyfin" nur als Kompatibilitätsangabe, klare Unofficial-Kennzeichnung.

---

## Deutsch (de-DE)

**App-Titel** (26/30 Zeichen):

> Finwatch – Jellyfin-Client

**Kurzbeschreibung** (73/80 Zeichen):

> Inoffizieller Jellyfin-Client für Wear OS. Eigener Jellyfin-Server nötig.

**Langbeschreibung:**

> Finwatch bringt deine Jellyfin-Mediathek auf die Smartwatch: Musik,
> Hörbücher, Serien und Filme direkt von deinem eigenen Server – ganz ohne
> Handy in der Nähe (Standalone-Wear-OS-App).
>
> VORAUSSETZUNG: ein eigener, erreichbarer Jellyfin-Server. Finwatch ist nur
> der Client – die App stellt keine Inhalte bereit und funktioniert ohne
> Server nicht.
>
> FUNKTIONEN
> • Anmeldung an deinem Jellyfin-Server; das Zugriffstoken wird lokal mit dem
>   Android-Keystore verschlüsselt gespeichert
> • Kompakter Startbildschirm mit konfigurierbaren Kategorien: Musik,
>   Hörbücher, Serien, Filme, Favoriten, Playlists
> • Bibliotheken, Interpreten/Alben und Ordner durchstöbern – mit
>   Shuffle-Wiedergabe auf jeder Ebene
> • Audio- und Videowiedergabe (ExoPlayer/Media3); Wiedergabe läuft im
>   Hintergrund mit Systemsteuerung weiter, auch bei ausgeschaltetem Display
> • Automatische Wiederaufnahme nach kurzen Netzwerkaussetzern
> • Wiedergabe wahlweise über Bluetooth-Kopfhörer oder den Uhr-Lautsprecher
> • Optionales serverseitiges Transcoding für Video; Audio spielt direkt ab
> • Anpassbar: Dunkel/Hell, Akzent- und Schriftfarben, Cover-Anzeige;
>   Sprachen: Deutsch, Englisch, Französisch, Spanisch, Arabisch
>
> PRIVATSPHÄRE
> Kein Tracking, keine Analytics, keine Werbung. Deine Zugangsdaten gehen
> ausschließlich an den Server, den du selbst einträgst; der Entwickler
> erhält keinerlei Daten. Datenschutzerklärung: https://srz.one/finwatch/privacy
>
> OPEN SOURCE
> Finwatch ist freie Software (MPL-2.0). Quellcode, Fehlerberichte und
> Mitarbeit: https://github.com/Phat-shot/finwatch
>
> HINWEIS: Finwatch ist ein inoffizieller Client und steht in keiner
> Verbindung zum Jellyfin-Projekt und wird von diesem weder unterstützt noch
> beworben. Jellyfin ist eine Marke des Jellyfin-Projekts.

---

## English (en-US)

**App title** (26/30 characters):

> Finwatch — Jellyfin client

**Short description** (74/80 characters):

> Unofficial Jellyfin client for Wear OS. Requires your own Jellyfin server.

**Full description:**

> Finwatch puts your Jellyfin library on your smartwatch: music, audiobooks,
> shows and movies straight from your own server — no phone needed nearby
> (standalone Wear OS app).
>
> REQUIREMENT: your own, reachable Jellyfin server. Finwatch is only the
> client — the app provides no content and does not work without a server.
>
> FEATURES
> • Sign in to your Jellyfin server; the access token is stored locally,
>   encrypted with the Android Keystore
> • Compact home screen with configurable categories: Music, Audiobooks,
>   Series, Movies, Favorites, Playlists
> • Browse libraries, artists/albums and folders — with shuffle play on
>   every screen
> • Audio and video playback (ExoPlayer/Media3); playback keeps running in
>   the background with system media controls, even with the screen off
> • Auto-recovery from brief network drops
> • Play through Bluetooth headphones or the watch's built-in speaker
> • Optional server-side transcoding for video; audio always direct-plays
> • Customizable: dark/light theme, accent and font colors, cover art
>   display; languages: English, German, French, Spanish, Arabic
>
> PRIVACY
> No tracking, no analytics, no ads. Your credentials go exclusively to the
> server you configure yourself; the developer receives no data at all.
> Privacy policy: https://srz.one/finwatch/privacy
>
> OPEN SOURCE
> Finwatch is free software (MPL-2.0). Source code, bug reports and
> contributions: https://github.com/Phat-shot/finwatch
>
> NOTE: Finwatch is an unofficial client. It is not affiliated with,
> endorsed or supported by the Jellyfin project. Jellyfin is a trademark of
> the Jellyfin project.

---

## Weitere Listing-Pflichtangaben (zur Erinnerung, vgl. Issue #15)

| Asset/Feld | Anforderung |
|---|---|
| App-Icon | 512 × 512 px, eigenes Finwatch-Logo (NICHT das Jellyfin-Logo; eigener Umriss, Jellyfin-Farbverlauf wäre laut Branding-Policy erlaubt) |
| Feature-Grafik | 1024 × 500 px |
| Wear-Screenshots | 1–8 Stück, 1:1, min. 384 × 384 px, echte App-UI ohne Watch-Rahmen/Deko-Text |
| Privacy-Policy-URL | https://srz.one/finwatch/privacy (Pflichtfeld) |
| Kategorie | Video Players & Editors oder Music & Audio |
| Kontakt-E-Mail | [KONTAKT-E-MAIL] |
