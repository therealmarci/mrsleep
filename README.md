# MRsleep

**Faire Schlafquote für Multiplayer-Server:** Wer nicht mitschlafen kann oder will, bremst die Nacht nicht mehr aus, und mit `/nosleep` hält man die Nacht fest, wenn man sie für eine Farm braucht.

**[⬇ Download](https://mrtechnology.marcelr.at/download/mrsleep/)** · **[📖 Anleitung online](https://mrtechnology.marcelr.at/docs/mrsleep/)** · **[🐞 Fehler melden](https://mrtechnology.marcelr.at/report/mrsleep/)**

| Auf einen Blick | |
|---|---|
| Server | Paper 26.3 (oder ein Fork wie Purpur), Java 25 |
| Benötigt | nichts - LuckPerms ist empfohlen, MRadmin optional |
| Sprachen | Deutsch, Englisch - jeder Spieler automatisch in der Sprache seines Minecraft-Clients |
| Status | stabil (1.x) |

## Was kann MRsleep?
- **Schlafquote mit Ausnahmen:** Bots, Cam-Accounts, Zuschauer und AFK-Spieler zählen nicht mit. Die Nacht wird übersprungen, sobald genug *echte* Spieler schlafen.
- **`/nosleep`:** Ein Spieler blockiert das Schlafen für eine oder mehrere Nächte - perfekt für Mob-Farmen oder Bauprojekte bei Nacht.
- **Admin-Menü:** Alle Spieler als Köpfe mit Live-Status. Sperren, Cooldown aufheben oder Blockaden lösen geht per Klick.
- **Anzeige nach Wahl:** Fortschritt als Bossbar, Actionbar oder Titel, dazu Chat-Hinweise wie „Alex hat sich schlafen gelegt (1/2)“.
- **Alles einstellbar:** Eine `config.yml`, alle Texte in Sprachdateien, für jede Funktion eine eigene Permission.
- **Discord-Logs** über einen Webhook und eine **Update-Prüfung**, die dich im Spiel informiert.

## Schnellstart
1. **[MRsleep herunterladen](https://mrtechnology.marcelr.at/download/mrsleep/)** und die JAR in den Ordner `plugins/` legen.
2. Den Server starten. Dabei entstehen `plugins/MRsleep/config.yml` und die Sprachdateien in `plugins/MRsleep/lang/`.
3. **Rechte geben** - Spieler brauchen sie für `/nosleep`, das Team für das Admin-Menü:
   ```text
   /lp group default permission set mrsleep.player true
   /lp group supporter permission set mrsleep.groups.support true
   /lp group staff permission set mrsleep.groups.staff true
   /lp group admin permission set mrsleep.groups.admin true
   ```
4. Fertig - die Standardquote ist 50 %. Wer etwas anpassen will: Config ändern und `/mrsleep reload` ausführen.

> [!TIP]
> Nutzt du LuckPerms mit `*`-Rechten für Admins? Dann lies kurz den Hinweis zu [Wildcards](#permissions), sonst zählen deine Admins nicht zur Quote.

**Update:** Die alte JAR durch die neue ersetzen und den Server neu starten. Ändert sich der Aufbau der Config, passt MRsleep sie automatisch an: Deine Einstellungen bleiben, neue Optionen kommen mit Standardwerten dazu, und die alte Datei wird als `config.v<N>.backup.yml` gesichert.

---

## Funktionen

### Schlafquote mit Ausnahmen
Spieler, die nicht zählen, werden **komplett aus der Rechnung genommen**:

> Quote 50 %, 6 Spieler online, 2 davon sind Bots mit `mrsleep.exempt` → es zählen 4 Spieler → **2 müssen schlafen**.

Nicht mitgezählt werden:
- Spieler mit der Permission `mrsleep.exempt` (z.B. Bots und Cam-Accounts)
- Zuschauer (`sleep.ignore-spectators`)
- Spieler, die ein AFK-Plugin als „sleeping ignored“ markiert (`sleep.respect-sleeping-ignored`)
- Spieler im Admin-Modus von MRadmin und Spieler im Duell bei MRpvp - siehe [Zusammenarbeit mit anderen Plugins](#zusammenarbeit-mit-anderen-plugins)
- Dummy-Spieler von MRchunks

Wer nicht zählt, bekommt beim Hinlegen einen Hinweis mit dem Grund. Wie in Vanilla wird die Nacht übersprungen, wenn die nötigen Spieler etwa 5 Sekunden im Bett liegen.

Verwaltet werden alle normalen Oberwelten. Nether und End sind ausgenommen, weitere Welten schließt du über `sleep.disabled-worlds` aus.

<details>
<summary>Warum steht die Gamerule <code>playersSleepingPercentage</code> auf 100?</summary>

Vanilla kann Spieler nicht wirklich aus der Rechnung nehmen - „ignorierte“ Spieler zählen dort weiter zur Gesamtzahl. MRsleep rechnet deshalb selbst: In verwalteten Welten steht die Gamerule auf `100`, die eigentliche Quote steht in `sleep.percentage`. Das ist gewollt, bitte nicht ändern.
</details>

### `/nosleep` - die Nacht festhalten
| Eingabe | Wirkung |
|---|---|
| `/nosleep` | Blockiert die laufende Nacht (tagsüber: die kommende). Ist schon eine eigene Blockade aktiv, wird sie **aufgehoben**. |
| `/nosleep 3` | Blockiert die nächsten 3 Nächte (höchstens `nosleep.max-nights`). |
| `/nosleep off` | Hebt die eigene Blockade auf (auch `aus` oder `cancel`). |
| `/nosleep info` | Zeigt deinen Status: wer schläft, ob du zählst, wer blockiert, dein Cooldown. |

- Solange **irgendein** Spieler blockiert, wird die Nacht nicht übersprungen. Alle anderen sehen, wer blockiert.
- **Cooldown** (Standard `15m`, `nosleep.cooldown`): Er startet erst, **wenn die blockierte Nacht anbricht**. Wer tagsüber blockiert und vor Nachtbeginn wieder aufhebt, bekommt keinen Cooldown. Auch OPs haben ihn - außer sie bekommen `mrsleep.nosleep.nocooldown`.
- Geht der blockierende Spieler offline, endet seine Blockade (`nosleep.release-on-quit`).
- Admins können den Cooldown eines Spielers auf Zeit aufheben (**Bypass**) oder Spieler von `/nosleep` **sperren**.
- Blockaden, Cooldowns, Bypässe und Sperren überstehen einen Neustart.

### Anzeige & Chat-Hinweise
- **Fortschritt** als Bossbar (Standard), Actionbar, Titel oder gar nicht (`sleep.display`). Die Bossbar wird vom Chat im Bett nicht verdeckt.
- Die eingebaute Minecraft-Anzeige „x/y Spieler schlafen“ in der Actionbar zählt alle Spieler und kennt die Regeln von MRsleep nicht. MRsleep ersetzt sie deshalb sofort durch den eigenen Fortschritt (bei `display: actionbar`) bzw. blendet sie aus (`sleep.hide-vanilla-message`, Standard an).
- **Schlaf-Nachricht** „Alex hat sich schlafen gelegt (1/2)“ an die Welt, den ganzen Server oder niemanden (`sleep.sleep-message`).
- Hinweise, wenn die Nacht blockiert ist, wenn sie übersprungen wurde („Guten Morgen!“), wenn eine Blockade abläuft und wenn ein Admin etwas ändert.
- Jeder Text steht in der Sprachdatei und lässt sich ändern oder mit `""` abschalten.

### Admin-Menü
`/mrsleep gui` öffnet eine Übersicht, die sich jede Sekunde aktualisiert:
- **Welt:** Uhrzeit, wie viele zählen, schlafen und gebraucht werden, wer blockiert. Ein Klick wechselt zur nächsten Welt.
- **Spielerköpfe:** Blockierer leuchten. Der Tooltip zeigt, ob jemand zählt (und warum nicht), schläft, blockiert, Cooldown oder eine Sperre hat.
- **Filter:** Alle, Schlafende, Blockierer, Ausgenommene, Gesperrte.

Ein Klick auf einen Kopf öffnet das **Spieler-Menü**: Bypass für 30m / 2h / 1d, Cooldown zurücksetzen, Sperren für 1h / 1d / dauerhaft, Blockade aufheben, zum Spieler teleportieren. Jede Aktion braucht ihre eigene Permission - fehlt sie, steht „Keine Berechtigung“ am Button. Die Zeiten und Sounds stellst du unter `gui` in der Config ein.

**Wie lange das Team darf** (`limits` in der Config - gilt für Befehle, Menü und die MRadmin-Weboberfläche):

| | Bypass höchstens | Sperre höchstens |
|---|---|---|
| Support (`mrsleep.admin.bypass` / `.ban`) | `limits.support.bypass` (Standard 30 Min) | `limits.support.ban` (Standard 1 Tag) |
| Staff (`.bypass.staff` / `.ban.staff`) | `limits.staff.bypass` (Standard 1 Tag) | `limits.staff.ban` (Standard 1 Tag) |
| Admin (`.bypass.unlimited` / `.ban.unlimited`) | unbegrenzt | unbegrenzt, auch dauerhaft |

Knöpfe mit einer längeren Dauer zeigen „Keine Berechtigung“, ein zu langer Befehl sagt, wie lange man höchstens darf.

---

## Befehle

| Befehl | Was er tut | Permission |
|---|---|---|
| `/nosleep [nächte\|off]` | Schlafen blockieren oder eigene Blockade aufheben | `mrsleep.nosleep` |
| `/nosleep info` | Eigener Schlaf- und Blockade-Status | `mrsleep.nosleep.info` |
| `/mrsleep gui` | Admin-Menü öffnen | `mrsleep.admin.gui` |
| `/mrsleep status [welt]` | Kurzer Schlafstatus | `mrsleep.admin.status` |
| `/mrsleep debug` | Details pro Welt und Spieler - **erste Anlaufstelle bei Problemen** | `mrsleep.admin.status` |
| `/mrsleep clear [welt]` | Alle Blockaden einer Welt entfernen | `mrsleep.admin.clear` |
| `/mrsleep release <spieler>` | Blockade eines Spielers aufheben | `mrsleep.admin.clear` |
| `/mrsleep bypass <spieler> <dauer>` | Cooldown für eine Zeit aufheben (`30m`, `2h`, `1d12h`) - höchstens so lange wie deine [Grenze](#admin-menü) | `mrsleep.admin.bypass` |
| `/mrsleep bypass remove <spieler>` · `bypass list` | Bypass entfernen · alle anzeigen | `mrsleep.admin.bypass` |
| `/mrsleep resetcooldown <spieler>` | Cooldown sofort zurücksetzen | `mrsleep.admin.resetcooldown` |
| `/mrsleep ban <spieler> [dauer\|permanent] [grund]` | Von `/nosleep` ausschließen (ohne Dauer: dauerhaft, braucht `.ban.unlimited`) - höchstens so lange wie deine [Grenze](#admin-menü) | `mrsleep.admin.ban` |
| `/mrsleep unban <spieler>` · `bans` | Sperre aufheben · alle Sperren anzeigen | `mrsleep.admin.ban` |
| `/mrsleep discord [test]` | Discord-Status anzeigen, Testnachricht senden | `mrsleep.admin.discord` |
| `/mrsleep reload` | Config und Sprachdatei neu laden | `mrsleep.admin.reload` |
| `/mrsleep version` | Version und Update-Status | alle |

Alle Befehle haben Tab-Vervollständigung. Bypass, Sperren und Cooldown-Reset funktionieren auch bei Spielern, die gerade offline sind.

## Permissions

| Permission | Standard | Wofür |
|---|---|---|
| **Gruppen** | | in allen MRtechnology-Plugins gleich aufgebaut, jede enthält die darunter |
| `mrsleep.player` | OP | `/nosleep` und `/nosleep info` |
| `mrsleep.groups.support` | OP | Spieler + Admin-Menü, Status, Teleport, Bypass und Sperre bis zur Support-Grenze |
| `mrsleep.groups.staff` | OP | Support + Bypass und Sperre bis zur Staff-Grenze, Blockaden aufheben, Cooldown zurücksetzen |
| `mrsleep.groups.admin` | OP | Alles: Staff + Bypass und Sperre ohne Grenze (auch dauerhaft), Reload, Discord, Update-Hinweis |
| **Einzeln** | | |
| `mrsleep.exempt` | niemand | Zählt **nicht** zur Schlafquote (Bots, Cam-Accounts) - in keiner Gruppe |
| `mrsleep.nosleep` | OP | `/nosleep` benutzen |
| `mrsleep.nosleep.info` | OP | `/nosleep info` benutzen |
| `mrsleep.nosleep.nocooldown` | niemand | `/nosleep` ohne Cooldown (auch OPs haben das nicht automatisch) - in keiner Gruppe |
| `mrsleep.admin.gui` | OP | Admin-Menü öffnen |
| `mrsleep.admin.status` | OP | `status`, `debug` |
| `mrsleep.admin.clear` | OP | `clear`, `release` (auch im Menü) |
| `mrsleep.admin.bypass` | OP | `bypass` bis `limits.support.bypass` (auch im Menü) |
| `mrsleep.admin.bypass.staff` · `.bypass.unlimited` | OP | Bypass bis `limits.staff.bypass` · ohne Grenze |
| `mrsleep.admin.resetcooldown` | OP | `resetcooldown` (auch im Menü) |
| `mrsleep.admin.ban` | OP | `ban` bis `limits.support.ban`, `unban`, `bans` (auch im Menü) |
| `mrsleep.admin.ban.staff` · `.ban.unlimited` | OP | Sperre bis `limits.staff.ban` · ohne Grenze und dauerhaft |
| `mrsleep.admin.teleport` | OP | Teleport-Button im Menü |
| `mrsleep.admin.reload` | OP | `reload` (auch im Menü) |
| `mrsleep.admin.update` | OP | Update-Hinweis beim Betreten des Servers |
| `mrsleep.admin.discord` | OP | `/mrsleep discord` |

> [!WARNING]
> **Wildcards:** Gruppen mit `*` oder `mrsleep.*` bekommen in LuckPerms auch `mrsleep.exempt` - und zählen dann nicht zur Quote. Setz für solche Gruppen `mrsleep.exempt` ausdrücklich auf `false`:
> `/lp group admin permission set mrsleep.exempt false`

<details>
<summary>Weitere LuckPerms-Beispiele</summary>

```text
/lp user CamAccount permission set mrsleep.exempt true          # Bot aus der Quote nehmen
/lp group mod permission set mrsleep.groups.staff true          # Mods: wie Staff …
/lp group mod permission set mrsleep.admin.ban.unlimited true   # … aber auch dauerhaft sperren
/lp group vip permission set mrsleep.nosleep.nocooldown true    # VIPs ohne Cooldown
/lp group guest permission set mrsleep.nosleep false            # Gäste ohne /nosleep
```
</details>

---

## Konfiguration
Alles steht in `plugins/MRsleep/config.yml`, die wichtigsten Einstellungen ganz oben. Jede Option ist in der Datei kommentiert.

| Abschnitt | Die wichtigsten Einstellungen |
|---|---|
| `sleep` | `percentage` (Quote, Standard 50), `minimum-sleepers`, `disabled-worlds`, `display` (bossbar/actionbar/title/none), `hide-vanilla-message`, `sleep-message`, wer nicht zählt |
| `nosleep` | `cooldown` (15m), `max-nights` (3), `prevent-bed-enter`, `release-on-quit`, `broadcast` |
| `discord` | Webhook-URL, welche Ereignisse gesendet werden, Farben |
| `update-checker` | an/aus, Prüfintervall, Hinweis beim Betreten |
| `gui` | Aktualisierung, Schnellauswahl-Zeiten, Sounds |
| `limits` | Wie lange Support und Staff höchstens Bypass geben und sperren dürfen |

- **Zeitangaben:** `d`, `h`, `m`, `s` kombinierbar (`15m`, `1h30m`, `2d`). Eine reine Zahl zählt als Minuten.
- **Änderungen übernehmen:** `/mrsleep reload` - ein Neustart ist nicht nötig.
- **`config-version`** bitte nicht ändern - daran erkennt MRsleep, ob die Datei aktualisiert werden muss.
- **Umlaute kaputt** („Ã¤“ statt „ä“)? Stell die Kodierung im Editor auf **UTF-8**.

### Sprache & Texte
- **Jeder Spieler bekommt seine Sprache:** Mit `language: auto` (Standard) richtet sich MRsleep nach der Sprache, die der Spieler in Minecraft eingestellt hat - Deutsch (auch Österreich und Schweiz) bekommt Deutsch, Englisch bekommt Englisch. Für Sprachen ohne eigene Datei gilt `fallback-language` (Standard Englisch). Mit `language: de` oder `en` bekommen alle dieselbe Sprache.
- **Server-Sprache:** Discord-Logs haben keinen einzelnen Empfänger - sie kommen in `server-language` (Standard `en`). Auf einem deutschen Server also `server-language: de` setzen.
- Die Konsole ist immer Englisch.
- Alle Texte (Chat, Anzeige, Menüs, Discord) stehen in `plugins/MRsleep/lang/<sprache>.yml` im [MiniMessage-Format](https://docs.advntr.dev/minimessage/format.html). Ein leerer Text (`""`) schaltet die Nachricht ab.
- **Eigene Sprache:** `lang/en.yml` kopieren (z.B. als `lang/fr.yml`) und übersetzen - mit `language: auto` bekommen französische Spieler sie dann automatisch.
- Neue Texte aus Updates ergänzt MRsleep automatisch - deine Änderungen bleiben erhalten.

### Discord-Webhook
1. In Discord: **Kanal bearbeiten → Integrationen → Webhooks → Neuer Webhook** und die **Webhook-URL kopieren**.
2. In der Config `discord.enabled: true` setzen und die URL bei `webhook-url` eintragen.
3. `/mrsleep reload`, dann mit **`/mrsleep discord test`** prüfen - der Befehl sagt dir, ob die Nachricht ankam oder warum nicht.

Unter `discord.events` legst du pro Ereignis fest, ob es gesendet wird und in welcher Farbe (z.B. `nosleep-activated`, `night-skipped`, `admin-ban`). Die Texte stehen in der Sprachdatei unter `discord`. `@everyone` löst nie einen Ping aus.

### Update-Prüfung
MRsleep prüft beim Start und danach alle paar Stunden (`update-checker.interval-hours`), ob es eine neue Version gibt:
- in der **Konsole** mit Download-Link,
- **im Spiel** für Spieler mit `mrsleep.admin.update` als anklickbarer Hinweis beim Betreten,
- jederzeit mit `/mrsleep version`.

Angeboten werden nur Versionen für **deine Minecraft-Version**. Wer eine Vorabversion nutzt, bekommt auch neuere Vorabversionen angezeigt.

---

## Zusammenarbeit mit anderen Plugins
MRsleep läuft **allein**. Zusammen mit anderen MRtechnology-Plugins kann es mehr - die Plugins sprechen dafür über **MRadmin** miteinander:

| Funktion | Braucht |
|---|---|
| MRsleep im Dashboard und Spieler-Menü von MRadmin, Seite **Schlaf** in der Weboberfläche | MRadmin |
| Spieler im Admin-Modus zählen nicht zur Quote (`sleep.ignore-admin-mode`) | MRadmin |
| Spieler im Duell zählen nicht, die Arena-Welt wird nie verwaltet (`sleep.ignore-pvp`) | MRadmin + MRpvp |
| Dummy-Spieler von MRchunks zählen nie | nichts (erkennt MRsleep selbst) |

## Häufige Fragen
**Ich liege im Bett, aber die Nacht wird nicht übersprungen.**
Führ `/mrsleep debug` aus oder öffne das Admin-Menü: Dort steht für jeden Spieler, ob er zählt (und warum nicht), ob er schläft und ob jemand blockiert. Häufigste Ursache: Über `*` hat ein Spieler auch `mrsleep.exempt` - siehe [Wildcards](#permissions).

**Die Gamerule `playersSleepingPercentage` steht auf 100.**
Das ist gewollt, die Quote kommt aus `sleep.percentage`.

**Was zählt als „eine Nacht“?**
Wer tagsüber `/nosleep` nutzt, blockiert die kommende Nacht, wer es nachts nutzt, die laufende. Blockaden gelten pro Welt.

**Ich habe einen Fehler gefunden oder einen Wunsch.**
Gern über das **[Formular auf der Website](https://mrtechnology.marcelr.at/report/mrsleep/)** - am besten mit der Ausgabe von `/mrsleep version` und `/mrsleep debug`.

---

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
