# Changelog

Alle wichtigen Änderungen an MRsleep. Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [Semantic Versioning](https://semver.org/lang/de/).

> **Pre-Release:** Alle 0.x-Versionen sind Testversionen. Bis zum Release 1.0.0 können sich Config,
> Befehle und Permissions noch ändern.

## [Unveröffentlicht]
### Intern
- Repo aufgeräumt: ungenutzten Code und Texte entfernt (`gui-yes`, `gui-no`), Versionslogik nach `util/Versions`
  ausgelagert. Vorabversionen werden jetzt je Teil verglichen (`rc.10` > `rc.2`).
- Unit-Tests (JUnit 6) für Zeitangaben und Versionsvergleich, laufen bei jedem Build und auf GitHub.
- `.editorconfig` (hält das BOM der `config.yml`), Issue-Vorlagen, Dependabot für GitHub-Actions.
- Doku: Inhaltsverzeichnis, durchnummerierte Abschnitte, Badges in der README.

## [0.9.4] - 2026-10-17
### Geändert
- **/nosleep-Cooldown startet erst bei Nachtbeginn** (ab dem Zeitpunkt, an dem man schlafen kann, auch bei Gewitter).
  Wer tagsüber blockiert und vor Nachtbeginn wieder aufhebt, bekommt keinen Cooldown. Aufheben in der Nacht
  bleibt möglich, der Cooldown läuft dann weiter. Blockieren in der Nacht startet den Cooldown sofort.
- Neue Hinweise: Cooldown startet bei Nachtbeginn, Cooldown läuft ab jetzt, Aufheben ohne Cooldown bzw. mit Restzeit.
- `/mrsleep debug` und das Admin-Menü zeigen „startet bei Nachtbeginn“ für wartende Blockaden.
- Config-Version 4 (neue Nachrichten, wird automatisch migriert).

### Neu
- **`/nosleep info`** für alle Spieler: Welt und Tag/Nacht, Schlafstand und ob man selbst zählt, wer blockiert,
  eigene Blockade, eigener Cooldown und Sperr-Status. Permission `mrsleep.nosleep.info` (Standard: alle).

## [0.9.3] - 2026-10-17
### Neu
- **Bossbar** für den Schlaf-Fortschritt (`sleep.display: bossbar`, Standard). Sie sitzt oben am Bildschirm und wird
  vom Chat-Fenster im Bett nicht mehr verdeckt. Der Balken zeigt den Fortschritt, bei einer Blockade ist sie rot.
  Alternativ `actionbar`, `title` oder `none`. Farben über `sleep.bossbar-color` und `sleep.bossbar-blocked-color`.
- `sleep.sleep-message`: Die Meldung „<Spieler> hat sich schlafen gelegt“ geht wahlweise an die Welt
  (`world`, Standard), den ganzen Server (`server`) oder an niemanden (`none`).

### Geändert
- Admin-Menü: Das Info-Buch zeigt „Version: Aktuell / Veraltet“ statt „Update: aktuell“.
- `/mrsleep version`: neue Reihenfolge (Version, Update-Prüfung, Ergebnis, Leerzeile, Links)
  und „Du nutzt die aktuellste Version.“
- Config-Version 3: `sleep.action-bar` wurde durch `sleep.display` ersetzt, `messages.actionbar-*` heißt jetzt
  `messages.display-*`. Bestehende Configs werden automatisch migriert.

## [0.9.2] - 2026-10-17
### Neu
- **Admin-Menü** (`/mrsleep gui`): Spielerköpfe mit Live-Status (zählt, schläft, blockiert, Cooldown, Sperre),
  Filter, Seiten, Weltübersicht und ein Aktionsmenü pro Spieler (Bypass, Cooldown-Reset, Sperren,
  Blockade aufheben, Teleport).
- **Sperren für /nosleep**: `/mrsleep ban <spieler> [dauer|permanent] [grund]`, `/mrsleep unban`, `/mrsleep bans`.
  Permission `mrsleep.admin.ban`.
- `/mrsleep release <spieler>` hebt die Blockade eines einzelnen Spielers auf.
- **Update-Checker** (Standard: GitHub Releases, alternativ Modrinth): Hinweis in der Konsole und für Spieler mit `mrsleep.admin.update`.
  Auf einer Vorabversion werden auch neuere Vorabversionen gemeldet.
- `/mrsleep version` zeigt Version, Links und Update-Status.
- Discord-Events `admin-ban`, `admin-unban`, `admin-release`.
- Lizenz (source-available, siehe `LICENSE`).

### Geändert
- `config.yml` neu strukturiert: wichtigste Einstellungen oben, Nachrichten am Ende.
  Bestehende Configs werden automatisch migriert, die alte Datei wird als `config.v1.backup.yml` gesichert.
- `config.yml` wird als UTF-8 mit BOM gespeichert, damit Editoren die Umlaute korrekt anzeigen.

## [0.9.1] - 2026-10-17
### Behoben
- Beim Hinlegen erschien „Keine noch so lange Rast kann diese Nacht verkürzen“, weil die Gamerule auf 101 stand.
  Sie steht jetzt auf 100.
- OPs hatten automatisch `mrsleep.nosleep.nocooldown`, wodurch der Cooldown für sie nie galt.
  Die Permission muss jetzt explizit vergeben werden.

### Neu
- Hinweis beim Hinlegen, wenn ein Spieler nicht zur Quote zählt (mit Grund).
- `/mrsleep debug` mit Status pro Welt und Spieler, inklusive Cooldown-Stand.

## [0.9.0] - 2026-10-17
### Neu
- Schlafquote mit Ausnahmen (`mrsleep.exempt`).
- `/nosleep [nächte]` mit Cooldown und Admin-Bypass, bis zu `max-nights` Nächte auf einmal.
- Discord-Webhook für Logs.
- Chat-Hinweise (Nachtbeginn, Ablauf der Blockade, Hinlegen, Join-Info …).
