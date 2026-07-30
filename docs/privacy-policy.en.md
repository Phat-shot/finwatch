<!--
  TEMPLATE / FOR ORIENTATION — not legal advice.
  Before publishing: fill in the [NAME/ADDRESS …] and philipp@srz.one placeholders
  and review the text yourself (or have it reviewed).
  Intended hosting location: https://srz.one/finwatch/privacy
-->

# Privacy Policy — Finwatch

*Last updated: 30 July 2026*

Finwatch is an unofficial open-source client for Jellyfin media servers on
Wear OS smartwatches. This policy describes what data the app processes — and,
above all, what it does not.

## Summary

- The developer operates **no servers of their own** and **receives no data
  whatsoever** from the app.
- The app connects exclusively to the **Jellyfin server that you configure
  yourself**. What that server stores is determined by its operator (usually
  you).
- **No tracking, no analytics, no advertising, no third-party SDKs** that
  collect data.

## Controller

Philipp Schwarz, Zur Sperrmauer 20, 34549 Edertal, Germany

Contact: philipp@srz.one

Because the app transmits no personal data to the developer, in normal operation
no processing of personal data within the meaning of the GDPR takes place on the
developer's side. This policy documents exactly that and explains the processing
that happens locally on your watch and towards your own server.

## What data the app processes

**Sign-in data (server URL, username, password).**
When you sign in, your username and password are transmitted once, directly to
the Jellyfin server you entered — and to no one else. Your password is not
stored. The server issues an access token; this token is stored locally on the
watch, encrypted with a key held in the Android Keystore (never in plaintext).

**Using the app (browsing, playback).**
While browsing and playing, the app sends the requests required for those
features (library queries, streaming requests, playback progress) to your
Jellyfin server. This data is exchanged solely between your watch and your
server and is used only to make the app work. Whatever the server logs or
stores (e.g. play history) is the responsibility of the server operator.

**Settings.**
Your app settings (appearance, playback, visible libraries, language) are
stored locally on the watch only.

**Legal basis** (to the extent the GDPR applies to this local processing at
all): the processing is necessary to provide the features you use
(Art. 6(1)(b) GDPR).

## What the app does NOT do

- no collection or transmission of data to the developer or to third parties,
- no tracking, no analytics, no crash reports sent to the developer,
- no advertising, no advertising IDs,
- no location data, no contacts, no sensor data,
- no account creation — the app does not create user accounts; it only signs in
  to an existing account on your own Jellyfin server.

## Transport encryption (HTTPS)

Whether the connection to your Jellyfin server is encrypted depends on the
server URL you enter. **We strongly recommend an `https://` address.** For
compatibility (e.g. servers on a home network), the app also allows
unencrypted `http://` connections; in that case, credentials and media are
transmitted unencrypted. Securing the server is the server operator's
responsibility.

## Android Auto Backup

The app participates in Android's Auto Backup. As a result, the server URL,
username, and app settings may be included in your device's Google account
backup (Google Drive); that backup is encrypted by Google. The access token is
part of the app data but is encrypted with a **device-bound** Keystore key and
is therefore unusable on any other device. You can disable backup in your
device's Android/Google settings.

## App permissions

- **Internet** — to connect to your Jellyfin server.
- **Notifications / foreground service (media playback)** — so playback keeps
  running with system media controls when the screen turns off.
- **Wake lock** — prevents playback from being cut off by power saving.

## Your rights

Under the GDPR you have, vis-à-vis a controller, in particular the rights of
access (Art. 15), rectification (Art. 16), erasure (Art. 17), restriction
(Art. 18), data portability (Art. 20) and objection (Art. 21), as well as the
right to lodge a complaint with a data protection supervisory authority
(Art. 77).

Since the developer stores no personal data about you, there is normally
nothing to disclose or erase on the developer's side. In practice:

- **Delete local data:** clear the app's data in the watch settings, or
  uninstall the app.
- **Data on the server:** contact the operator of your Jellyfin server
  (usually yourself).

For any questions, contact philipp@srz.one.

## Changes

This policy will be updated as needed (e.g. for new features). The current
version is always available at this address; the date of the last change is
shown at the top.

---

*Finwatch is an independent open-source project (MPL-2.0,
[github.com/Phat-shot/finwatch](https://github.com/Phat-shot/finwatch)) and is
not affiliated with the Jellyfin project. Jellyfin is a trademark of the
Jellyfin project.*
