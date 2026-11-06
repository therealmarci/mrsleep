# Quick start

1. **[Download MRsleep](https://mrtechnology.marcelr.at/download/mrsleep/)** and put the JAR into the `plugins/` folder.
2. Start the server. This creates `plugins/MRsleep/config.yml` and the language files in `plugins/MRsleep/lang/`.
3. **Give permissions** - players need them for `/nosleep`, the team for the admin menu:
   ```text
   /lp group default permission set mrsleep.player true
   /lp group supporter permission set mrsleep.groups.support true
   /lp group staff permission set mrsleep.groups.staff true
   /lp group admin permission set mrsleep.groups.admin true
   ```
4. Done - the default quota is 50 %. To adjust something: change the config and run `/mrsleep reload` (or configure it in the browser with MRadmin, see [Configuration](konfiguration.md)).

> [!WARNING]
> Do you use LuckPerms with `*` permissions for admins? Then quickly read the note on [wildcards](permissions.md#wildcards), otherwise your admins are not counted for the quota.

## Update
Replace the old JAR with the new one and restart the server. If the layout of the config changes, MRsleep adapts it automatically: your settings stay, new options are added with their defaults, and the old file is backed up as `config.v<N>.backup.yml`. More under [Install & update](https://mrtechnology.marcelr.at/wiki/allgemein/installieren/).
