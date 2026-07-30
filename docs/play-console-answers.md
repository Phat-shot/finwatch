# Play-Console-Ausfüllhilfe — Finwatch

Konkrete Antwortlinien für die Pflichtformulare unter **Play Console → App
content / App-Inhalte**. *Orientierung, keine Rechtsberatung.* Die genauen
Feldbezeichnungen ändern sich in der Console gelegentlich; wo unklar, ist das
Feld generisch beschrieben.

---

## 1. Data-Safety-Formular ("Datensicherheit")

**Empfohlene Linie: "Keine Daten erhoben, keine Daten geteilt."**

Begründung: "Erheben" (collect) heißt laut Google-Definition, dass Nutzerdaten
das Gerät verlassen und an den Entwickler oder von ihm eingebundene
SDKs/Drittserver übertragen werden. Finwatch überträgt Daten ausschließlich an
den **vom Nutzer selbst eingetragenen** Jellyfin-Server — einen Empfänger, den
der Entwickler weder betreibt noch kennt; es gibt keine SDKs, die Daten
senden. Zusätzlich greift die Ausnahme der **ephemeren Verarbeitung**:
Anfragen (Login, Streaming, Wiedergabestatus) werden nur zur unmittelbaren
Funktionserfüllung verarbeitet und vom Entwickler nirgends gespeichert. So
deklarieren es etablierte Self-Hosted-Clients (offizielle Jellyfin-App,
Nextcloud-Apps, diverse Jellyfin-Drittclients auch im App Store: "No data
collected").

| Frage (sinngemäß) | Antwort | Begründung |
|---|---|---|
| Erhebt oder teilt Ihre App die erforderlichen Nutzerdatentypen? | **Nein** | Keine Übertragung an Entwickler/Dritte; Kommunikation nur Gerät ↔ nutzereigener Server; ephemere Verarbeitung |
| Werden alle Nutzerdaten bei der Übertragung verschlüsselt? | entfällt bei "Nein" oben | siehe Hinweis unten |
| Können Nutzer die Löschung ihrer Daten verlangen? | entfällt bei "Nein" oben | lokale Daten: App-Daten löschen/deinstallieren |
| Unabhängige Sicherheitsüberprüfung (MASA, optional) | Nein | freiwillig, nicht nötig |

**Hinweis Verschlüsselung in transit:** Falls Google im Review doch eine
Deklaration verlangt (Rückfall-Linie: "Benutzer-IDs/Passwörter — erhoben, Zweck
App-Funktionalität, nicht geteilt, ephemer verarbeitet, Löschung durch
Deinstallation"), ist die Frage "verschlüsselt übertragen?" heikel: Die App
erlaubt neben HTTPS auch `http://`-Server-URLs (Heimnetz), die Verschlüsselung
hängt also vom nutzereigenen Server ab. Dann ehrlich **"Nein"** (bzw. "nicht
alle") wählen und in der Datenschutzerklärung erläutern — oder vor Einreichung
Cleartext-HTTP technisch abschaffen/absichern (networkSecurityConfig, vgl.
Issue #15), dann ist uneingeschränkt "Ja" möglich.

**Account-Löschung (separate Frage im Bereich Datensicherheit):** Finwatch
bietet **keine Kontoerstellung** in der App an (Login nur in bestehende Konten
auf dem nutzereigenen Server) → entsprechend "keine Kontoerstellung"
beantworten; dann ist keine Account-Deletion-URL nötig.

---

## 2. IARC-Altersfreigabe-Fragebogen ("Content ratings")

Kategorie: **Utility/Tool/Kommunikation o. Ä.** trifft es nicht — Finwatch ist
ein Media-Player; im Fragebogen die zutreffende App-Kategorie wählen und dann:

| Frage (sinngemäß) | Antwort |
|---|---|
| Gewalt, Sex, Drogen, Glücksspiel usw. *in der App selbst* | Nein (die App liefert keine eigenen Inhalte) |
| Zugriff auf das Internet / uneingeschränkte Webinhalte | **Ja** |
| Kann die App nutzergenerierte/unmoderierte Inhalte wiedergeben? | **Ja** (spielt beliebige, unmoderierte Medien vom nutzereigenen Server ab) |
| Teilen von Standort/persönlichen Daten mit Dritten | Nein |
| Digitale Käufe | Nein |

**Erwartetes Ergebnis:** Wegen "unmoderierte Inhalte + freier Internetzugriff"
ergibt sich üblicherweise keine "0/Everyone"-Einstufung, sondern etwas wie
USK 12/16, PEGI 12+ bzw. "Parental Guidance"/"Teen" — das ist für einen
Self-Hosted-Media-Client normal und korrekt. **Nicht schönfärben**: Wer hier
"keine bedenklichen Inhalte" ankreuzt, riskiert bei Beschwerden eine
Neubewertung/Sperre (vgl. Issue #15/#20).

---

## 3. DSA-Trader-Deklaration ("Händlerstatus" nach Digital Services Act)

**Antwort: Non-Trader / "Ich bin kein Unternehmer".**

Begründung: Finwatch ist kostenlos, ohne Werbung, ohne In-App-Käufe, ohne
jegliche Monetarisierung; die Bereitstellung erfolgt privat/hobbymäßig ohne
Erwerbszweck — damit kein "Trader" im Sinne des DSA. Konsequenz: keine
verifizierte Anschrift/Telefonnummer im Store-Listing nötig (beim
Trader-Status müsste eine geprüfte Adresse öffentlich angezeigt werden).
**Neu bewerten**, falls später Monetarisierung (Spenden-Buttons im Store,
Käufe, Werbung) dazukommt.

---

## 4. US-Export / Verschlüsselung

In der Play Console gibt es — anders als bei Apple — **kein eigenes
Export-/Verschlüsselungsformular**; die Pflicht zur Einhaltung der
US-Exportgesetze ergibt sich aus der Developer Distribution Agreement
(Play-Hilfe-Artikel "Export compliance", answer/113770). Einordnung für
Finwatch: nur **Standard-Kryptografie der Plattform** (TLS/HTTPS über
Android-Systembibliotheken, Android Keystore) → gilt als Standard-/
Mass-Market-Verschlüsselung und ist nach EAR üblicherweise ausgenommen
(License Exception ENC); keine eigene/nicht-standardisierte Kryptografie,
keine gesonderte Meldung nötig. Die einzige Console-Stelle, die
Verschlüsselung abfragt, ist die "in transit"-Frage im Data-Safety-Formular
(siehe oben). Falls in einem Formular doch eine Frage "Uses encryption?"
auftaucht: **"Ja — nur Standardverschlüsselung (exempt)"** wählen.

---

## 5. App-Zugriff für das Review ("App access")

Finwatch ist ohne Jellyfin-Server nicht nutzbar → im Formular **"Der gesamte
Zugriff oder Teile davon sind eingeschränkt"** wählen und Zugangsdaten für das
Review-Team hinterlegen.

**Option A — eigener Testserver (empfohlen):**
1. Kleinen Jellyfin-Server öffentlich erreichbar machen (VPS oder Heimserver
   hinter Reverse-Proxy/HTTPS), mit ein paar unverfänglichen, rechtefreien
   Medien (z. B. Blender-Open-Movies, CC-Musik).
2. Eigenes Reviewer-Konto anlegen (z. B. `playreview` + starkes Passwort,
   keine Adminrechte).
3. Im Formular "App access" eine Anleitung ("Instructions") hinterlegen, etwa:
   - Server URL: `https://test.srz.one` (Beispiel)
   - Username: `playreview` / Password: `…`
   - "Open the app, choose 'Sign in', enter the values above."
4. Server während des gesamten Review-Zeitraums (und für spätere
   Update-Reviews!) erreichbar lassen — Google prüft Zugänge auch später
   erneut.

**Option B — öffentlicher Jellyfin-Demo-Server (nur Notlösung):**
`https://demo.jellyfin.org/stable` mit Benutzer `demo` und **leerem Passwort**
funktioniert grundsätzlich als Jellyfin-Instanz. Für das Play-Review aber
**nicht empfohlen**: Der Server gehört dem Jellyfin-Projekt (Drittanbieter,
keine Verfügbarkeits-Garantie), es gab wiederholt Berichte über nicht
funktionierende Demo-Logins/Ausfälle, und ein leeres Passwort passt evtl.
nicht durch den Login-Flow der App. Wenn überhaupt, dann nur zusätzlich als
Fallback in den Instructions nennen — die Hauptzugangsdaten sollten auf einen
selbst kontrollierten Server zeigen.

---

## 6. Übrige "App content"-Erklärungen (Kurzüberblick)

| Formular | Antwort |
|---|---|
| Privacy-Policy-URL | `https://srz.one/finwatch/privacy` |
| Zielgruppe ("Target audience") | Nur Erwachsene bzw. 18+ wählen (nicht an Kinder gerichtet); dadurch entfallen die Families-Anforderungen |
| Anzeigen ("Ads") | Nein, enthält keine Werbung |
| News-App | Nein |
| COVID-19-Tracing/-Status | Nein |
| Datenschutz-/Gesundheitsdaten, Finanz-Features, staatliche App | jeweils Nein |
| Wear-OS-Formfaktor | Opt-in setzen (App content → Formfaktoren); zusätzliches Wear-Quality-Review einplanen (Issue #15) |
