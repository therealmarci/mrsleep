# Befehle

## Für Spieler
| Befehl | Was er tut | Permission |
|---|---|---|
| `/nosleep [nächte\|off]` | Schlafen blockieren oder eigene Blockade aufheben | `mrsleep.nosleep` |
| `/nosleep info` | Eigener Schlaf- und Blockade-Status | `mrsleep.nosleep.info` |
| `/mrsleep version` | Version und Update-Status | `mrsleep.version` (in `mrsleep.player`) |

## Fürs Team
| Befehl | Was er tut | Permission |
|---|---|---|
| `/mrsleep gui` | [Admin-Menü](admin-menue.md) öffnen | `mrsleep.admin.gui` |
| `/mrsleep status [welt]` | Kurzer Schlafstatus | `mrsleep.admin.status` |
| `/mrsleep debug` | Details pro Welt und Spieler - **erste Anlaufstelle bei Problemen** | `mrsleep.admin.status` |
| `/mrsleep clear [welt]` | Alle Blockaden einer Welt entfernen | `mrsleep.admin.clear` |
| `/mrsleep release <spieler>` | Blockade eines Spielers aufheben | `mrsleep.admin.clear` |
| `/mrsleep bypass <spieler> <dauer>` | Cooldown für eine Zeit aufheben (`30m`, `2h`, `1d12h`) - höchstens so lange wie deine [Grenze](admin-menue.md#wie-lange-das-team-darf) | `mrsleep.admin.bypass` |
| `/mrsleep bypass remove <spieler>` · `bypass list` | Bypass entfernen · alle anzeigen | `mrsleep.admin.bypass` |
| `/mrsleep resetcooldown <spieler>` | Cooldown sofort zurücksetzen | `mrsleep.admin.resetcooldown` |
| `/mrsleep ban <spieler> [dauer\|permanent] [grund]` | Von `/nosleep` ausschließen (ohne Dauer: dauerhaft, braucht `.ban.unlimited`) - höchstens so lange wie deine [Grenze](admin-menue.md#wie-lange-das-team-darf) | `mrsleep.admin.ban` |
| `/mrsleep unban <spieler>` · `bans` | Sperre aufheben · alle Sperren anzeigen | `mrsleep.admin.ban` |
| `/mrsleep discord [test]` | Discord-Status anzeigen, Testnachricht senden | `mrsleep.admin.discord` |
| `/mrsleep reload` | Config und Sprachdatei neu laden | `mrsleep.admin.reload` |

Alle Befehle haben Tab-Vervollständigung. Bypass, Sperren und Cooldown-Reset funktionieren auch bei Spielern, die gerade offline sind.
