# Admin-Menü & Team-Grenzen

`/mrsleep gui` öffnet eine Übersicht, die sich jede Sekunde aktualisiert:
- **Welt:** Uhrzeit, wie viele zählen, schlafen und gebraucht werden, wer blockiert. Ein Klick wechselt zur nächsten Welt.
- **Spielerköpfe:** Blockierer leuchten. Der Tooltip zeigt, ob jemand zählt (und warum nicht), schläft, blockiert, Cooldown oder eine Sperre hat.
- **Filter:** Alle, Schlafende, Blockierer, Ausgenommene, Gesperrte.

## Spieler-Menü
Ein Klick auf einen Kopf öffnet das **Spieler-Menü**:
- Bypass für 30m / 2h / 1d (Cooldown aufheben)
- Cooldown zurücksetzen
- Sperren für 1h / 1d / dauerhaft
- Blockade aufheben
- zum Spieler teleportieren

Jede Aktion braucht ihre eigene [Permission](permissions.md) - fehlt sie, steht „Keine Berechtigung“ am Knopf. Die Zeiten und Sounds stellst du unter `gui` in der Config ein (`bypass-durations`, `ban-durations`, `sounds`, `refresh-ticks`).

## Wie lange das Team darf
`team-limits` in der Config - gilt für Befehle, Menü und die MRadmin-Weboberfläche:

| | Bypass höchstens | Sperre höchstens |
|---|---|---|
| Support (`mrsleep.admin.bypass` / `.ban`) | `team-limits.support.bypass` (Standard 30 Min) | `team-limits.support.ban` (Standard 1 Tag) |
| Staff (`.bypass.staff` / `.ban.staff`) | `team-limits.staff.bypass` (Standard 1 Tag) | `team-limits.staff.ban` (Standard 1 Tag) |
| Admin (`.bypass.unlimited` / `.ban.unlimited`) | unbegrenzt | unbegrenzt, auch dauerhaft |

Knöpfe mit einer längeren Dauer zeigen „Keine Berechtigung“, ein zu langer Befehl sagt, wie lange man höchstens darf. Bypass, Sperren und Cooldown-Reset funktionieren auch bei Spielern, die gerade offline sind.

> [!TIP]
> Mit MRadmin gibt es dasselbe auch im Browser: Seite **Schlaf** in der Weboberfläche und der Bereich MRsleep auf jeder Spielerseite.
