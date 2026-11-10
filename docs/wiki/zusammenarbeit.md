# Zusammenarbeit mit anderen Plugins

MRsleep läuft **allein**. Zusammen mit anderen MRtechnology-Plugins kann es mehr - die Plugins sprechen dafür über **MRadmin** miteinander ([wie das funktioniert](https://mrtechnology.marcelr.at/wiki/allgemein/zusammenarbeit/)).

| Funktion | Braucht |
|---|---|
| MRsleep im Dashboard und Spieler-Menü von MRadmin, Seite **Schlaf** in der Weboberfläche | MRadmin |
| MRsleep im Browser einstellen (Plugins → MRsleep → Konfiguration) | MRadmin |
| Spieler im Admin-Modus zählen nicht zur Quote (`sleep.ignore.admin-mode`) | MRadmin |
| Spieler im Duell zählen nicht, die Arena-Welt wird nie verwaltet (`sleep.ignore.pvp`) | MRadmin + MRpvp |
| Dummy-Spieler von MRchunks zählen nie | nichts (erkennt MRsleep selbst) |
| Ereignisse im **Protokoll** der MRadmin-Weboberfläche, Discord-Log über MRadmins Webhook (`discord.use-mradmin-webhook`) | MRadmin |
| Hinweise ans Team im Spiel (z.B. Update verfügbar) lassen sich unter Konto → Benachrichtigungen ein- und ausschalten und mit `/mradmin mute` stummschalten | MRadmin |
