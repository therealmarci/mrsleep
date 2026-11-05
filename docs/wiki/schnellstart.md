# Schnellstart

1. **[MRsleep herunterladen](https://mrtechnology.marcelr.at/download/mrsleep/)** und die JAR in den Ordner `plugins/` legen.
2. Den Server starten. Dabei entstehen `plugins/MRsleep/config.yml` und die Sprachdateien in `plugins/MRsleep/lang/`.
3. **Rechte geben** - Spieler brauchen sie für `/nosleep`, das Team für das Admin-Menü:
   ```text
   /lp group default permission set mrsleep.player true
   /lp group supporter permission set mrsleep.groups.support true
   /lp group staff permission set mrsleep.groups.staff true
   /lp group admin permission set mrsleep.groups.admin true
   ```
4. Fertig - die Standardquote ist 50 %. Wer etwas anpassen will: Config ändern und `/mrsleep reload` ausführen (oder mit MRadmin im Browser einstellen, siehe [Konfiguration](konfiguration.md)).

> [!WARNING]
> Nutzt du LuckPerms mit `*`-Rechten für Admins? Dann lies kurz den Hinweis zu [Wildcards](permissions.md#wildcards), sonst zählen deine Admins nicht zur Quote.

## Update
Die alte JAR durch die neue ersetzen und den Server neu starten. Ändert sich der Aufbau der Config, passt MRsleep sie automatisch an: Deine Einstellungen bleiben, neue Optionen kommen mit Standardwerten dazu, und die alte Datei wird als `config.v<N>.backup.yml` gesichert. Mehr dazu unter [Installieren & aktualisieren](https://mrtechnology.marcelr.at/wiki/allgemein/installieren/).
