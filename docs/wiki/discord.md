# Discord & Protokoll

## Discord-Webhook einrichten
1. In Discord: **Kanal bearbeiten → Integrationen → Webhooks → Neuer Webhook** und die **Webhook-URL kopieren**.
2. In der Config `discord.enabled: true` setzen und die URL bei `discord.webhook-url` eintragen.
3. `/mrsleep reload`, dann mit **`/mrsleep discord test`** prüfen - der Befehl sagt dir, ob die Nachricht ankam oder warum nicht.

Unter `discord.events` legst du pro Ereignis fest, ob es gesendet wird und in welcher Farbe. Die Texte stehen in der Sprachdatei unter `discord` (in `general.server-language`). `@everyone` löst nie einen Ping aus.

| Ereignis | Wann |
|---|---|
| `nosleep-activated` · `nosleep-deactivated` | Spieler blockiert bzw. hebt auf |
| `nosleep-quit` · `nosleep-expired` | Blockade endet durch Verlassen bzw. läuft ab |
| `night-start-blocked` · `bed-blocked` | Nacht beginnt blockiert · jemand will in einer blockierten Nacht schlafen |
| `night-skipped` | Nacht übersprungen |
| `admin-clear` · `admin-release` | Team entfernt Blockaden |
| `admin-bypass` · `admin-bypass-remove` · `admin-cooldown-reset` | Cooldown aufgehoben bzw. zurückgesetzt |
| `admin-ban` · `admin-unban` | Sperre von `/nosleep` |
| `admin-reload` | Config neu geladen |

Weitere Einstellungen: `username`, `avatar-url`, `use-embeds`, `footer`.

## Ohne eigenen Webhook
> [!TIP]
> Mit MRadmin geht es auch über dessen Discord-Log: `discord.enabled: true`, `webhook-url` leer lassen und `discord.use-mradmin-webhook: true` setzen.

## Protokoll in MRadmin
Mit MRadmin steht jedes Ereignis außerdem im **Protokoll** der Weboberfläche (bis zu 14 Tage) - auch wenn Discord aus ist.
