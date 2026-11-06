# MRsleep

**Faire Schlafquote für Multiplayer-Server:** Wer nicht mitschlafen kann oder will, bremst die Nacht nicht mehr aus, und mit `/nosleep` hält man die Nacht fest, wenn man sie für eine Farm braucht.

**[⬇ Download](https://mrtechnology.marcelr.at/download/mrsleep/)** · **[📖 Wiki](https://mrtechnology.marcelr.at/wiki/mrsleep/)** · **[🐞 Fehler melden](https://mrtechnology.marcelr.at/report/mrsleep/)**

| Auf einen Blick | |
|---|---|
| Server | Paper 26.3 (oder ein Fork wie Purpur), Java 25 |
| Benötigt | nichts - LuckPerms ist empfohlen, MRadmin optional |
| Sprachen | Deutsch, Englisch - jeder Spieler automatisch in der Sprache seines Minecraft-Clients |
| Status | stabil (1.x) |

## Was kann MRsleep?
- **Schlafquote mit Ausnahmen:** Bots, Cam-Accounts, Zuschauer und AFK-Spieler zählen nicht mit.
- **`/nosleep`:** Ein Spieler blockiert das Schlafen für eine oder mehrere Nächte - perfekt für Mob-Farmen.
- **Admin-Menü:** Alle Spieler mit Live-Status, Sperren und Cooldowns per Klick.
- **Anzeige nach Wahl:** Bossbar, Actionbar oder Titel, dazu kurze Chat-Hinweise.
- **Alles einstellbar**, Discord-Logs und Update-Prüfung - mit MRadmin auch im Browser.

## Schnellstart
1. **[MRsleep herunterladen](https://mrtechnology.marcelr.at/download/mrsleep/)** und die JAR in den Ordner `plugins/` legen.
2. Den Server starten.
3. Rechte geben:
   ```text
   /lp group default permission set mrsleep.player true
   /lp group supporter permission set mrsleep.groups.support true
   /lp group staff permission set mrsleep.groups.staff true
   /lp group admin permission set mrsleep.groups.admin true
   ```
4. Fertig - die Standardquote ist 50 %.

**Update:** alte JAR durch die neue ersetzen, Server neu starten - die Config passt sich selbst an.

## Ganze Anleitung im Wiki
| Für Spieler | Für Admins |
|---|---|
| [Schlafen & Schlafquote](https://mrtechnology.marcelr.at/wiki/mrsleep/schlafen/) | [Befehle](https://mrtechnology.marcelr.at/wiki/mrsleep/befehle/) · [Permissions](https://mrtechnology.marcelr.at/wiki/mrsleep/permissions/) · [Konfiguration](https://mrtechnology.marcelr.at/wiki/mrsleep/konfiguration/) |
| [/nosleep - die Nacht festhalten](https://mrtechnology.marcelr.at/wiki/mrsleep/nosleep/) | [Admin-Menü](https://mrtechnology.marcelr.at/wiki/mrsleep/admin-menue/) · [Discord](https://mrtechnology.marcelr.at/wiki/mrsleep/discord/) · [Häufige Fragen](https://mrtechnology.marcelr.at/wiki/mrsleep/faq/) |

Die Seiten liegen als Markdown in [`docs/wiki/`](docs/wiki/) (Seitenbaum: [`docs/wiki/index.yml`](docs/wiki/index.yml)).

## Lizenz
MRsleep steht unter der **[MRtechnology Lizenz](LICENSE)** (source-available, nicht Open Source). Kurz gesagt:

| ✅ Erlaubt | ❌ Nicht erlaubt |
|---|---|
| Nutzen - privat und gewerblich, auch auf Servern mit Spenden, Rängen oder Shops | Weiterveröffentlichen (auch verändert), z.B. auf Modrinth, Spigot, in Modpacks |
| Verändern und neu kompilieren | Verkaufen oder gegen Geld anbieten |
| An dein Team oder deinen Hoster weitergeben | Als eigenes Werk ausgeben, Lizenzhinweis entfernen |
| Verbesserungen per Pull Request einreichen | |

Die Nutzung erfolgt auf eigenes Risiko - leg vor Updates eine Sicherung an. Maßgeblich ist der vollständige Text in [LICENSE](LICENSE).

## Für Entwickler
Selbst bauen mit Java 25: `gradlew build` → `build/libs/MRsleep-mc<minecraft>-v<version>.jar`. Versionen heißen `mc<minecraft>-v<version>` (z.B. `mc26.3-v1.1.5`). Aufbau des Codes: [docs/DOKUMENTATION.md](docs/DOKUMENTATION.md), Branches und Releases: [docs/RELEASES.md](docs/RELEASES.md).
