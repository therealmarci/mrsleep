# Permissions

## Gruppen
In allen MRtechnology-Plugins gleich aufgebaut, jede enthält die darunter (mehr unter [Rechte mit LuckPerms](https://mrtechnology.marcelr.at/wiki/allgemein/rechte/)).

| Permission | Standard | Wofür |
|---|---|---|
| `mrsleep.player` | OP | `/nosleep`, `/nosleep info`, `/mrsleep version` |
| `mrsleep.groups.support` | OP | Spieler + Admin-Menü, Status, Teleport, Bypass und Sperre bis zur Support-Grenze |
| `mrsleep.groups.staff` | OP | Support + Bypass und Sperre bis zur Staff-Grenze, Blockaden aufheben, Cooldown zurücksetzen |
| `mrsleep.groups.admin` | OP | Alles: Staff + Bypass und Sperre ohne Grenze (auch dauerhaft), Reload, Discord, Update-Hinweis |

## Einzeln
| Permission | Standard | Wofür |
|---|---|---|
| `mrsleep.exempt` | niemand | Zählt **nicht** zur Schlafquote (Bots, Cam-Accounts) - in keiner Gruppe |
| `mrsleep.nosleep` | OP | `/nosleep` benutzen |
| `mrsleep.nosleep.info` | OP | `/nosleep info` benutzen |
| `mrsleep.nosleep.nocooldown` | niemand | `/nosleep` ohne Cooldown (auch OPs haben das nicht automatisch) - in keiner Gruppe |
| `mrsleep.version` | OP | `/mrsleep version` |
| `mrsleep.admin.gui` | OP | Admin-Menü öffnen |
| `mrsleep.admin.status` | OP | `status`, `debug` |
| `mrsleep.admin.clear` | OP | `clear`, `release` (auch im Menü) |
| `mrsleep.admin.bypass` | OP | `bypass` bis `team-limits.support.bypass` (auch im Menü) |
| `mrsleep.admin.bypass.staff` · `.bypass.unlimited` | OP | Bypass bis `team-limits.staff.bypass` · ohne Grenze |
| `mrsleep.admin.resetcooldown` | OP | `resetcooldown` (auch im Menü) |
| `mrsleep.admin.ban` | OP | `ban` bis `team-limits.support.ban`, `unban`, `bans` (auch im Menü) |
| `mrsleep.admin.ban.staff` · `.ban.unlimited` | OP | Sperre bis `team-limits.staff.ban` · ohne Grenze und dauerhaft |
| `mrsleep.admin.teleport` | OP | Teleport-Knopf im Menü |
| `mrsleep.admin.reload` | OP | `reload` (auch im Menü) |
| `mrsleep.admin.update` | OP | Update-Hinweis beim Betreten des Servers |
| `mrsleep.admin.discord` | OP | `/mrsleep discord` |

## Wildcards
> [!WARNING]
> Gruppen mit `*` oder `mrsleep.*` bekommen in LuckPerms auch `mrsleep.exempt` - und zählen dann nicht zur Quote. Setz für solche Gruppen `mrsleep.exempt` ausdrücklich auf `false`:
> `/lp group admin permission set mrsleep.exempt false`

## Beispiele
```text
/lp user CamAccount permission set mrsleep.exempt true          # Bot aus der Quote nehmen
/lp group mod permission set mrsleep.groups.staff true          # Mods: wie Staff …
/lp group mod permission set mrsleep.admin.ban.unlimited true   # … aber auch dauerhaft sperren
/lp group vip permission set mrsleep.nosleep.nocooldown true    # VIPs ohne Cooldown
/lp group guest permission set mrsleep.nosleep false            # Gäste ohne /nosleep
```
