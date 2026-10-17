# Changelog

Alle wichtigen Änderungen an MRsleep. Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [Semantic Versioning](https://semver.org/lang/de/).

## [1.2.0] - 2026-10-17
### Neu
- **Admin-Menü** (`/mrsleep gui`): Spielerköpfe mit Live-Status (zählt, schläft, blockiert, Cooldown, Sperre),
  Filter, Seiten, Weltübersicht und ein Aktionsmenü pro Spieler (Bypass, Cooldown-Reset, Sperren,
  Blockade aufheben, Teleport).
- **Sperren für /nosleep**: `/mrsleep ban <spieler> [dauer|permanent] [grund]`, `/mrsleep unban`, `/mrsleep bans`.
  Permission `mrsleep.admin.ban`.
- `/mrsleep release <spieler>` hebt die Blockade eines einzelnen Spielers auf.
- **Update-Checker** (Modrinth oder GitHub): Hinweis in der Konsole und für Spieler mit `mrsleep.admin.update`.
- `/mrsleep version` zeigt Version, Links und Update-Status.
- Discord-Events `admin-ban`, `admin-unban`, `admin-release`.

### Geändert
- `config.yml` neu strukturiert: wichtigste Einstellungen oben, Nachrichten am Ende.
  Bestehende Configs werden automatisch migriert, die alte Datei wird als `config.v1.backup.yml` gesichert.
- `config.yml` wird als UTF-8 mit BOM gespeichert, damit Editoren die Umlaute korrekt anzeigen.

## [1.1.2] - 2026-10-17
### Behoben
- OPs hatten automatisch `mrsleep.nosleep.nocooldown`, wodurch der Cooldown für sie nie galt.
  Die Permission muss jetzt explizit vergeben werden.

### Neu
- `/mrsleep debug` zeigt den Cooldown-Stand pro Spieler.

## [1.1.1] - 2026-10-17
### Behoben
- Beim Hinlegen erschien „Keine noch so lange Rast kann diese Nacht verkürzen“, weil die Gamerule auf 101 stand.
  Sie steht jetzt auf 100.

### Neu
- Hinweis beim Hinlegen, wenn ein Spieler nicht zur Quote zählt (mit Grund).
- `/mrsleep debug`.

## [1.1.0] - 2026-10-17
### Neu
- `/nosleep [nächte]` blockiert bis zu `max-nights` Nächte auf einmal.
- Discord-Webhook für Logs.
- Chat-Hinweise (Nachtbeginn, Ablauf der Blockade, Hinlegen, Join-Info …).

## [1.0.0] - 2026-10-17
### Neu
- Schlafquote mit Ausnahmen (`mrsleep.exempt`).
- `/nosleep` mit Cooldown und Admin-Bypass.
