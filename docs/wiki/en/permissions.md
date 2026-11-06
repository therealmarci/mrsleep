# Permissions

How the groups are built in all plugins: [Permissions with LuckPerms](https://mrtechnology.marcelr.at/wiki/allgemein/rechte/).

## Groups
Every group contains the one below, the team groups also contain `mrsleep.player` - best give `mrsleep.player` to the default group:

<!-- auto:permission-groups -->

```text
/lp group default permission set mrsleep.player true
/lp group supporter permission set mrsleep.groups.support true
/lp group staff permission set mrsleep.groups.staff true
/lp group admin permission set mrsleep.groups.admin true
```

## Single permissions
"Included in" names the smallest group that brings the permission; "–" means: in no group, only given on its own.

<!-- auto:permissions -->

## Wildcards
> [!WARNING]
> Groups with `*` or `mrsleep.*` also get `mrsleep.exempt` in LuckPerms - and are then not counted for the quota. For such groups set `mrsleep.exempt` to `false` explicitly:
> `/lp group admin permission set mrsleep.exempt false`

## Examples
```text
/lp user CamAccount permission set mrsleep.exempt true          # take a bot out of the quota
/lp group mod permission set mrsleep.groups.staff true          # mods: like staff …
/lp group mod permission set mrsleep.admin.ban.unlimited true   # … but may also ban permanently
/lp group vip permission set mrsleep.nosleep.nocooldown true    # VIPs without cooldown
/lp group guest permission set mrsleep.nosleep false            # guests without /nosleep
```
