# Working with other plugins

MRsleep runs **on its own**. Together with other MRtechnology plugins it can do more - the plugins talk to each other through **MRadmin** ([how that works](https://mrtechnology.marcelr.at/wiki/allgemein/zusammenarbeit/)).

| Feature | Needs |
|---|---|
| MRsleep in MRadmin's dashboard and player menu, page **Sleep** in the web interface | MRadmin |
| Configure MRsleep in the browser (Plugins → MRsleep → Configuration) | MRadmin |
| Players in admin mode are not counted for the quota (`sleep.ignore.admin-mode`) | MRadmin |
| Players in a duel are not counted, the arena world is never managed (`sleep.ignore.pvp`) | MRadmin + MRpvp |
| Dummy players of MRchunks are never counted | nothing (MRsleep detects them itself) |
| Events in the **log** of the MRadmin web interface, Discord log via MRadmin's webhook (`discord.use-mradmin-webhook`) | MRadmin |
| Messages to the team in the game (e.g. update available) can be switched on and off under Account → Notifications and muted with `/mradmin mute` | MRadmin |
