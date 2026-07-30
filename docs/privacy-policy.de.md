<!--
  VORLAGE / ORIENTIERUNG — keine Rechtsberatung.
  Vor Veröffentlichung: Platzhalter [NAME/ADRESSE …] und [KONTAKT-E-MAIL] ausfüllen
  und den Text einmal selbst (ggf. fachkundig) prüfen.
  Vorgesehener Hosting-Ort: https://srz.one/finwatch/privacy
-->

# Datenschutzerklärung — Finwatch

*Stand: 30. Juli 2026*

Finwatch ist ein inoffizieller Open-Source-Client für Jellyfin-Medienserver auf
Wear-OS-Smartwatches. Diese Erklärung beschreibt, welche Daten die App
verarbeitet — und vor allem, welche nicht.

## Das Wichtigste in Kürze

- Der Entwickler betreibt **keine eigenen Server** und **erhält keinerlei Daten**
  aus der App.
- Die App verbindet sich ausschließlich mit dem **Jellyfin-Server, den Sie selbst
  eintragen**. Was dieser Server speichert, bestimmt dessen Betreiber (in der
  Regel Sie selbst).
- **Kein Tracking, keine Analyse-Dienste, keine Werbung, keine
  Drittanbieter-SDKs**, die Daten sammeln.

## Verantwortlicher

[NAME/ADRESSE — vom Betreiber einzusetzen]

Kontakt: [KONTAKT-E-MAIL]

Da die App keine personenbezogenen Daten an den Entwickler übermittelt, findet
beim Entwickler im Regelbetrieb keine Verarbeitung personenbezogener Daten im
Sinne der DSGVO statt. Diese Erklärung dokumentiert genau das und erläutert die
Verarbeitung, die lokal auf Ihrer Uhr bzw. gegenüber Ihrem eigenen Server
stattfindet.

## Welche Daten die App verarbeitet

**Anmeldedaten (Server-URL, Benutzername, Passwort).**
Bei der Anmeldung werden Benutzername und Passwort einmalig direkt an den von
Ihnen eingetragenen Jellyfin-Server übermittelt — an niemanden sonst. Ihr
Passwort wird nicht gespeichert. Der Server stellt ein Zugriffstoken aus; dieses
wird lokal auf der Uhr gespeichert und dabei mit einem Schlüssel aus dem
Android-Keystore verschlüsselt (nicht im Klartext).

**Nutzung der App (Bibliothek, Wiedergabe).**
Beim Stöbern und Abspielen sendet die App die dafür nötigen Anfragen
(z. B. Bibliotheksabfragen, Streaming-Anfragen, Wiedergabestatus) an Ihren
Jellyfin-Server. Diese Daten dienen ausschließlich der Funktion der App und
werden nur zwischen Ihrer Uhr und Ihrem Server ausgetauscht. Was der Server
davon protokolliert oder speichert (z. B. Abspielverlauf), liegt in der
Verantwortung des Serverbetreibers.

**Einstellungen.**
Ihre App-Einstellungen (Darstellung, Wiedergabe, angezeigte Bibliotheken,
Sprache) werden nur lokal auf der Uhr gespeichert.

**Rechtsgrundlage** (soweit die DSGVO auf die lokale Verarbeitung überhaupt
anwendbar ist): Die Verarbeitung ist zur Bereitstellung der von Ihnen genutzten
Funktionen erforderlich (Art. 6 Abs. 1 lit. b DSGVO).

## Was die App NICHT tut

- keine Erhebung oder Übermittlung von Daten an den Entwickler oder Dritte,
- kein Tracking, keine Analytics, keine Absturzberichte an den Entwickler,
- keine Werbung, keine Werbe-IDs,
- keine Standortdaten, keine Kontakte, keine Sensordaten,
- keine Kontoerstellung — die App legt keine Benutzerkonten an, sondern meldet
  sich nur an einem bestehenden Konto Ihres eigenen Jellyfin-Servers an.

## Transportverschlüsselung (HTTPS)

Ob die Verbindung zu Ihrem Jellyfin-Server verschlüsselt ist, hängt von der
Server-URL ab, die Sie eintragen. **Wir empfehlen dringend eine
`https://`-Adresse.** Die App lässt aus Kompatibilitätsgründen (z. B. Server im
Heimnetz) auch unverschlüsselte `http://`-Verbindungen zu; in diesem Fall werden
Anmeldedaten und Medien unverschlüsselt übertragen. Die Absicherung des Servers
liegt beim Serverbetreiber.

## Android Auto-Backup

Die App nimmt am Auto-Backup von Android teil. Dabei können Server-URL,
Benutzername und App-Einstellungen in das Google-Konto-Backup Ihres Geräts
(Google Drive) aufgenommen werden; dieses Backup ist von Google verschlüsselt.
Das Zugriffstoken ist zwar Teil der App-Daten, aber mit einem
**gerätegebundenen** Keystore-Schlüssel verschlüsselt und daher auf einem
anderen Gerät nicht verwendbar. Das Backup können Sie in den
Android-/Google-Einstellungen Ihres Geräts deaktivieren.

## Berechtigungen der App

- **Internet** — Verbindung zu Ihrem Jellyfin-Server.
- **Benachrichtigungen / Vordergrunddienst (Medienwiedergabe)** — damit die
  Wiedergabe mit Systemsteuerung weiterläuft, wenn das Display ausgeht.
- **Wake Lock** — verhindert, dass die Wiedergabe durch den Energiesparmodus
  abbricht.

## Ihre Rechte

Nach der DSGVO haben Sie gegenüber einem Verantwortlichen insbesondere das Recht
auf Auskunft (Art. 15), Berichtigung (Art. 16), Löschung (Art. 17),
Einschränkung (Art. 18), Datenübertragbarkeit (Art. 20) und Widerspruch
(Art. 21) sowie das Recht auf Beschwerde bei einer Datenschutz-Aufsichtsbehörde
(Art. 77).

Da der Entwickler keine personenbezogenen Daten von Ihnen speichert, gibt es
beim Entwickler in der Regel nichts zu beauskunften oder zu löschen. Praktisch
gilt:

- **Lokale Daten löschen:** App-Daten in den Einstellungen der Uhr löschen oder
  die App deinstallieren.
- **Daten auf dem Server:** wenden Sie sich an den Betreiber Ihres
  Jellyfin-Servers (meist Sie selbst).

Bei Fragen erreichen Sie uns unter [KONTAKT-E-MAIL].

## Änderungen

Diese Erklärung wird bei Bedarf angepasst (z. B. bei neuen Funktionen). Die
jeweils aktuelle Fassung finden Sie unter dieser Adresse; das Änderungsdatum
steht oben.

---

*Finwatch ist ein unabhängiges Open-Source-Projekt (MPL-2.0,
[github.com/Phat-shot/finwatch](https://github.com/Phat-shot/finwatch)) und
steht in keiner Verbindung zum Jellyfin-Projekt. Jellyfin ist eine Marke des
Jellyfin-Projekts.*
