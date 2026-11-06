# Permissions

Wie die Gruppen in allen Plugins aufgebaut sind: [Rechte mit LuckPerms](https://mrtechnology.marcelr.at/wiki/allgemein/rechte/).

## Gruppen
Jede Gruppe enthält die darunter, die Team-Gruppen auch `mrsleep.player` - am besten `mrsleep.player` der Default-Gruppe geben:

<!-- auto:permission-groups -->

```text
/lp group default permission set mrsleep.player true
/lp group supporter permission set mrsleep.groups.support true
/lp group staff permission set mrsleep.groups.staff true
/lp group admin permission set mrsleep.groups.admin true
```

## Einzeln
„Enthalten in“ nennt die kleinste Gruppe, die die Permission mitbringt; „–“ heißt: in keiner Gruppe, nur einzeln vergeben.

<!-- auto:permissions -->

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
