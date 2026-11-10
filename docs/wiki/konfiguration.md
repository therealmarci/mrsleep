# Konfiguration

Am einfachsten im Browser: Mit **MRadmin** unter **Plugins → MRsleep → Konfiguration** - jede Einstellung mit Erklärung, Speichern lädt MRsleep sofort neu (Permission `mradmin.config`). Ohne MRadmin steht alles in `plugins/MRsleep/config.yml`, jede Option ist dort kommentiert.

| Abschnitt | Die wichtigsten Einstellungen | Mehr dazu |
|---|---|---|
| `general` | `language` (Standard `auto`), `fallback-language`, `server-language` | [Sprachen & Texte](https://mrtechnology.marcelr.at/wiki/allgemein/sprachen/) |
| `sleep` | `percentage` (Quote, Standard 50), `minimum-sleepers`, `disabled-worlds`, `clear-weather`; `ignore` (wer nicht zählt) | [Schlafquote](schlafquote.md) |
| `sleep.display`, `sleep.chat` | Bossbar/Actionbar/Titel, Farben, Vanilla-Anzeige ausblenden; Schlaf-Nachricht, „Guten Morgen!“ | [Anzeige & Chat](anzeige.md) |
| `nosleep` | `cooldown` (15m), `max-nights` (3), `prevent-bed-enter`, `release-on-quit`, `broadcast` | [Schlafquote → /nosleep](schlafquote.md#nosleep-aus-admin-sicht) |
| `team-limits` | Wie lange Support und Staff höchstens Bypass geben und sperren dürfen | [Team-Grenzen](admin-menue.md#wie-lange-das-team-darf) |
| `discord` | Webhook-URL, welche Ereignisse gesendet werden, Farben | [Discord & Protokoll](discord.md) |
| `updates` | an/aus, Quelle, Prüfintervall, Hinweis beim Betreten | [Update-Prüfung](#update-prüfung) |
| `gui` | Aktualisierung, Schnellauswahl-Zeiten, Sounds | [Admin-Menü](admin-menue.md) |

- **Zeitangaben:** `d`, `h`, `m`, `s` kombinierbar (`15m`, `1h30m`, `2d`). Eine reine Zahl zählt als Minuten.
- **Änderungen übernehmen:** `/mrsleep reload` - ein Neustart ist nicht nötig.
- **`config-version`** bitte nicht ändern - daran erkennt MRsleep, ob die Datei aktualisiert werden muss.
- **Umlaute kaputt** („Ã¤“ statt „ä“)? Stell die Kodierung im Editor auf **UTF-8**.

## Sprache & Texte
- `general.language: auto` (Standard): Jeder Spieler bekommt die Sprache seines Minecraft-Clients, sonst `general.fallback-language` (Standard Englisch).
- Discord-Logs haben keinen einzelnen Empfänger - sie kommen in `general.server-language` (Standard `en`). Auf einem deutschen Server also `general.server-language: de` setzen.
- Alle Texte (Chat, Anzeige, Menüs, Discord) stehen in `plugins/MRsleep/lang/<sprache>.yml` im MiniMessage-Format. Ein leerer Text (`""`) schaltet die Nachricht ab.
- Eigene Sprache, Texte ändern und mehr: [Sprachen & Texte](https://mrtechnology.marcelr.at/wiki/allgemein/sprachen/).

## Update-Prüfung
MRsleep prüft beim Start und danach alle paar Stunden (`updates.interval-hours`, Standard 6), ob es eine neue Version gibt:
- in der **Konsole** mit Download-Link,
- **im Spiel** für Spieler mit `mrsleep.admin.update` als anklickbarer Hinweis beim Betreten (`updates.notify-on-join`),
- jederzeit mit `/mrsleep version`.

Angeboten werden nur Versionen für **deine Minecraft-Version**. Wer eine Vorabversion nutzt, bekommt auch neuere Vorabversionen angezeigt (sonst nur mit `updates.beta`).

## Alle Einstellungen
Jede Einstellung der `config.yml` mit Standardwert - dieselben Felder wie im Browser-Formular von MRadmin. Die Liste entsteht automatisch aus dem Plugin und ist damit immer auf dem Stand dieser Version.

<!-- auto:config -->
