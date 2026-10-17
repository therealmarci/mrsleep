# MRsleep

[![Build](https://github.com/therealmarci/mrsleep/actions/workflows/build.yml/badge.svg)](https://github.com/therealmarci/mrsleep/actions/workflows/build.yml)
[![Neueste Version](https://img.shields.io/github/v/release/therealmarci/mrsleep?include_prereleases&label=Version)](https://github.com/therealmarci/mrsleep/releases)
![Paper](https://img.shields.io/badge/Paper-26.3-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

Schlaf-Plugin für **Paper 26.3** (Java 25).

> 🧪 **Pre-Release (0.x):** MRsleep befindet sich in der Testphase. Bis zum Release 1.0.0 können sich Config,
> Befehle und Permissions noch ändern. Fehler bitte als [Issue](https://github.com/therealmarci/mrsleep/issues) melden.

- **Schlafquote mit Ausnahmen**: Cam- und Bot-Accounts zählen nicht mit.
- **`/nosleep [nächte]`**: Spieler blockieren das Schlafen für eine oder mehrere Nächte, mit Cooldown. `/nosleep info` zeigt den eigenen Status.
- **Bossbar** mit dem Schlaf-Fortschritt, die vom Chat im Bett nicht verdeckt wird.
- **Admin-Menü**: Spielerköpfe mit Live-Status, Sperren, Bypass und Cooldown per Klick.
- **Sperren**: Admins schließen einzelne Spieler von `/nosleep` aus, auf Zeit oder dauerhaft.
- **Discord-Logs**, **Chat-Hinweise** und eine **Update-Prüfung** über GitHub Releases (alternativ Modrinth).
- **Alles einstellbar** in einer `config.yml`. Für jede Funktion gibt es eine Permission.

---

## Inhalt
1. [Installation & Update](#installation--update)
2. [Funktionen](#funktionen)
3. [Admin-Menü](#admin-menü)
4. [Befehle](#befehle)
5. [Permissions](#permissions)
6. [Konfiguration](#konfiguration)
7. [Discord-Webhook](#discord-webhook)
8. [Update-Prüfung](#update-prüfung)
9. [LuckPerms-Beispiele](#luckperms-beispiele)
10. [Häufige Fragen](#häufige-fragen)
11. [Selbst bauen & Releases](#selbst-bauen--releases)

---

## Installation & Update
1. `MRsleep-<version>.jar` in den Ordner `plugins/` legen und den Server starten. Beim ersten Start entsteht `plugins/MRsleep/config.yml`.
2. Die Config anpassen und mit `/mrsleep reload` übernehmen.

**Update:** Die alte JAR durch die neue ersetzen und den Server neu starten. Hat sich der Aufbau der Config geändert, aktualisiert MRsleep sie **automatisch**:
- Deine Einstellungen werden übernommen.
- Neue Optionen kommen mit Standardwerten dazu.
- Die alte Datei bleibt als `config.v<N>.backup.yml` erhalten.

**Voraussetzungen:** Paper 26.3 und Java 25. LuckPerms ist empfohlen, aber jedes Permission-Plugin funktioniert.

---

## Funktionen

### Schlafquote mit Ausnahmen
Spieler mit `mrsleep.exempt` werden **komplett aus der Rechnung genommen**.

> Quote 50 %, 6 Spieler online, 2 davon mit `mrsleep.exempt` → es zählen 4 Spieler → **2 müssen schlafen**.

Vanilla kann das nicht. Dort zählen „ignorierte“ Spieler weiter zur Gesamtzahl. Deshalb rechnet MRsleep selbst:
- Die Gamerule `playersSleepingPercentage` steht in verwalteten Welten auf `100`, und den Vanilla-Skip fängt das Plugin ab.
- Die eigentliche Quote steht in `sleep.percentage`.
- Ebenfalls nicht gezählt werden Zuschauer und Spieler, die ein AFK-Plugin als „sleeping ignored“ markiert. Beides lässt sich abschalten.
- Wer nicht zählt, bekommt beim Hinlegen einen Hinweis mit dem Grund.
- Wie in Vanilla wird die Nacht erst übersprungen, wenn die nötigen Spieler etwa 5 Sekunden im Bett liegen.

Verwaltet werden alle normalen Oberwelten. Nether und End sind ausgenommen, weitere Welten lassen sich über `sleep.disabled-worlds` ausschließen.

### `/nosleep [nächte]`
| Eingabe | Wirkung |
|---|---|
| `/nosleep` | Blockiert die laufende (oder, wenn es Tag ist, die kommende) Nacht. Ist schon eine eigene Blockade aktiv, wird sie **aufgehoben**. |
| `/nosleep 3` | Blockiert die nächsten 3 Nächte (höchstens `nosleep.max-nights`) und ersetzt eine eigene Blockade. |
| `/nosleep off` | Hebt die eigene Blockade auf (auch `aus` oder `cancel`). |
| `/nosleep info` | Zeigt den eigenen Status: Schlafstand, ob man zählt, wer blockiert, eigene Blockade, Cooldown, Sperre. |

- Solange **irgendein** Spieler blockiert, wird die Nacht nicht übersprungen.
- **Cooldown** (`nosleep.cooldown`, Standard `15m`): Er **startet erst, wenn die blockierte Nacht anbricht**, also ab dem Zeitpunkt, an dem man schlafen kann (auch bei Gewitter).
  - Wer tagsüber blockiert und vor Nachtbeginn wieder aufhebt, bekommt **keinen** Cooldown.
  - In der Nacht lässt sich jederzeit aufheben, der Cooldown läuft dann aber weiter.
  - Wer nachts blockiert, startet den Cooldown sofort.
  - Auch OPs haben den Cooldown, außer man vergibt `mrsleep.nosleep.nocooldown` ausdrücklich.
- **Bypass:** Ein Admin kann den Cooldown eines Spielers für eine bestimmte Zeit aufheben.
- **Sperre:** Ein Admin kann einen Spieler auf Zeit oder dauerhaft von `/nosleep` ausschließen. Seine laufenden Blockaden werden dabei sofort aufgehoben.
- Geht der blockierende Spieler offline, endet seine Blockade (`nosleep.release-on-quit`).
- Blockaden, Cooldowns, Bypässe und Sperren bleiben auch nach einem Neustart erhalten.

### Chat-Hinweise
Alle Texte lassen sich in der Config ändern. Ein leerer Text (`""`) schaltet den Hinweis ab. Beispiele:
- `Steve hat das Schlafen blockiert, du kannst nicht schlafen. Noch 2 Nächte.`
- `Die Nacht bricht an - Schlafen ist blockiert von Steve (noch 2 Nächte).`
- `Alex hat sich schlafen gelegt (1/2)`: an die Welt, den ganzen Server oder niemanden (`sleep.sleep-message`)
- Fortschritt als **Bossbar** oben am Bildschirm (`sleep.display`). Sie wird vom Chat im Bett nicht verdeckt, alternativ gibt es Actionbar, Titel oder keine Anzeige.
- `Guten Morgen! Die Nacht wurde übersprungen.`
- Hinweise, wenn eine Blockade abläuft, wenn jemand den Server während einer Blockade betritt, und wenn ein Admin einen Spieler sperrt, entsperrt oder seinen Cooldown ändert.

---

## Admin-Menü
`/mrsleep gui` öffnet eine Übersicht, die sich jede Sekunde selbst aktualisiert.

```text
┌──────────────────────────── MRsleep » Übersicht ────────────────────────────┐
│  ▒  [Welt]  ▒   ▒  [Info]  ▒   ▒  [Blockaden entfernen]  ▒                   │
│  [Kopf] [Kopf] [Kopf] [Kopf] [Kopf] …        ← ein Kopf pro Online-Spieler     │
│  …                                                                           │
│  [«]  ▒  [Filter]  ▒  [Schließen]  ▒  [Reload]  ▒  [»]                        │
└──────────────────────────────────────────────────────────────────────────────┘
```

- **Welt** (Uhr oder Kompass): Uhrzeit, Tag/Nacht, wie viele zählen, schlafen und gebraucht werden, wer blockiert. Ein Klick wechselt zur nächsten Welt.
- **Spielerköpfe**: Sortiert nach Blockierern, Schlafenden, normalen und ausgenommenen Spielern. Wer blockiert, leuchtet. Der Tooltip zeigt: zählt zur Quote (mit Grund), schläft (seit wie vielen Sekunden), blockiert (wie viele Nächte), Cooldown, Bypass, Sperre.
- **Filter** (Trichter): Alle, Schlafende, Blockierer, Ausgenommene, Gesperrte.
- **Info** (Buch): Version, Update-Status, Quote, Cooldown.

Ein Klick auf einen Kopf öffnet das **Spieler-Menü**:

| Reihe | Aktionen |
|---|---|
| Cooldown | Bypass für 30m / 2h / 1d · Bypass entfernen · Cooldown zurücksetzen |
| Sperre | Sperren für 1h / 1d / dauerhaft · Entsperren |
| Sonstiges | Blockade aufheben · Zum Spieler teleportieren |

Für jede Aktion braucht man zusätzlich die passende Permission. Fehlt sie, steht „Keine Berechtigung“ am Button. Die Schnellauswahl-Zeiten, der Sperrgrund und die Sounds lassen sich unter `gui` in der Config ändern.

---

## Befehle

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/nosleep [nächte\|off]` | Schlafen blockieren oder die eigene Blockade aufheben | `mrsleep.nosleep` |
| `/nosleep info` | Eigener Status: Schlafstand, ob man zählt, wer blockiert, eigene Blockade, Cooldown, Sperre | `mrsleep.nosleep.info` |
| `/mrsleep` | Übersicht der Admin-Befehle | eine der `mrsleep.admin.*` |
| `/mrsleep gui` | Admin-Menü öffnen | `mrsleep.admin.gui` |
| `/mrsleep status [welt]` | Kurzer Schlafstatus | `mrsleep.admin.status` |
| `/mrsleep debug` | Details pro Welt und Spieler (Fehlersuche) | `mrsleep.admin.status` |
| `/mrsleep clear [welt]` | Alle Blockaden einer Welt entfernen | `mrsleep.admin.clear` |
| `/mrsleep release <spieler>` | Blockade eines Spielers aufheben | `mrsleep.admin.clear` |
| `/mrsleep bypass <spieler> <dauer>` | Cooldown für eine Dauer aufheben (`30m`, `2h`, `1d12h` …) | `mrsleep.admin.bypass` |
| `/mrsleep bypass remove <spieler>` / `bypass list` | Bypass entfernen oder alle anzeigen | `mrsleep.admin.bypass` |
| `/mrsleep resetcooldown <spieler>` | Cooldown sofort zurücksetzen | `mrsleep.admin.bypass` |
| `/mrsleep ban <spieler> [dauer\|permanent] [grund]` | Von `/nosleep` ausschließen (ohne Dauer: dauerhaft) | `mrsleep.admin.ban` |
| `/mrsleep unban <spieler>` | Sperre aufheben | `mrsleep.admin.ban` |
| `/mrsleep bans` | Alle Sperren anzeigen | `mrsleep.admin.ban` |
| `/mrsleep reload` | `config.yml` neu laden | `mrsleep.admin.reload` |
| `/mrsleep version` | Version, Links und Update-Status | – (alle) |

Alle Befehle haben Tab-Vervollständigung. Bypass, Sperren und Cooldown-Reset funktionieren auch bei Spielern, die gerade offline sind, sofern sie schon einmal auf dem Server waren.

---

## Permissions

| Permission | Standard | Beschreibung |
|---|---|---|
| `mrsleep.exempt` | niemand | Zählt **nicht** zur Schlafquote (Cam- und Bot-Accounts) |
| `mrsleep.nosleep` | alle | `/nosleep` benutzen |
| `mrsleep.nosleep.info` | alle | `/nosleep info` benutzen |
| `mrsleep.nosleep.nocooldown` | niemand | `/nosleep` ohne Cooldown (auch OPs haben das nicht automatisch) |
| `mrsleep.admin` | OP | Enthält alle `mrsleep.admin.*` |
| `mrsleep.admin.gui` | OP | Admin-Menü öffnen |
| `mrsleep.admin.status` | OP | `status`, `debug` |
| `mrsleep.admin.clear` | OP | `clear`, `release` und die entsprechenden Buttons im Menü |
| `mrsleep.admin.bypass` | OP | `bypass`, `resetcooldown` und die entsprechenden Buttons im Menü |
| `mrsleep.admin.ban` | OP | `ban`, `unban`, `bans` und die entsprechenden Buttons im Menü |
| `mrsleep.admin.teleport` | OP | Teleport-Button im Menü |
| `mrsleep.admin.reload` | OP | `reload` und der Reload-Button im Menü |
| `mrsleep.admin.update` | OP | Update-Hinweis beim Betreten des Servers |

> ⚠️ **Wildcards:** Gruppen mit `*` oder `mrsleep.*` bekommen in LuckPerms auch `mrsleep.exempt` und zählen dann nicht zur Quote. Setz dort `mrsleep.exempt` ausdrücklich auf `false` (siehe unten).

---

## Konfiguration
Die `plugins/MRsleep/config.yml` ist in Abschnitte gegliedert, die wichtigsten Einstellungen stehen oben:

| Abschnitt | Inhalt |
|---|---|
| **1) Schlafquote** `sleep.*` | `percentage` (50), `minimum-sleepers`, `disabled-worlds`, Zuschauer und AFK, Wetter, Anzeige (`display`: bossbar/actionbar/title/none), `sleep-message` |
| **2) /nosleep** `nosleep.*` | `cooldown` (15m), `max-nights` (3), `prevent-bed-enter`, `release-on-quit`, `broadcast` |
| **3) Discord** `discord.*` | Webhook-URL und die einzelnen Events |
| **4) Update-Check** `update-checker.*` | Quelle (GitHub Releases oder Modrinth), Intervall, Hinweis beim Betreten |
| **5) Admin-Menü** `gui.*` | Aktualisierung, Schnellauswahl-Zeiten, Sperrgrund, Sounds |
| **6) Nachrichten** `messages.*` | Alle Texte ([MiniMessage](https://docs.advntr.dev/minimessage/format.html)) |

- **Zeitangaben** bestehen aus `d`, `h`, `m` und `s` (`15m`, `1h30m`, `2d`). Eine reine Zahl zählt als Minuten.
- **Umlaute:** Die Datei ist UTF-8 mit BOM gespeichert, damit Editoren die Kodierung erkennen. Siehst du trotzdem „Ã¤“ statt „ä“, stell die Kodierung im Editor auf **UTF-8**.
- **`config-version`** nicht ändern. Darüber erkennt MRsleep, ob die Datei aktualisiert werden muss.

---

## Discord-Webhook
1. In Discord den Kanal öffnen und über **Kanal bearbeiten → Integrationen → Webhooks → Neuer Webhook** einen Webhook anlegen, dann die **Webhook-URL kopieren**.
2. In der Config `discord.enabled: true` setzen und die `webhook-url` eintragen.
3. `/mrsleep reload` ausführen.

Unter `discord.events` legst du für jedes Ereignis fest, ob es gesendet wird, welche Farbe es hat und welchen Text es bekommt:
- `nosleep-activated`, `nosleep-deactivated`, `nosleep-quit`, `nosleep-expired`
- `night-start-blocked`, `bed-blocked`, `night-skipped`
- `admin-clear`, `admin-release`, `admin-bypass`, `admin-bypass-remove`, `admin-cooldown-reset`, `admin-ban`, `admin-unban`, `admin-reload`

Gesendet wird im Hintergrund. Das Rate-Limit von Discord wird eingehalten, und `@everyone` löst nie einen Ping aus.

---

## Update-Prüfung
MRsleep prüft beim Start und danach alle `update-checker.interval-hours` Stunden, ob es eine neue Version gibt. Standardquelle sind die **GitHub Releases** (`update-checker.source: github`). Alternativ geht `modrinth`. Weil jedes Release automatisch auf beiden Plattformen erscheint, ist das Ergebnis gleich.
- **Konsole:** Ein auffälliger Hinweis mit installierter und neuer Version samt Download-Link.
- **Im Spiel:** Spieler mit `mrsleep.admin.update` bekommen beim Betreten einen anklickbaren Hinweis.
- `/mrsleep version` prüft sofort und zeigt das Ergebnis.

---

## LuckPerms-Beispiele
```text
/lp user CamAccount permission set mrsleep.exempt true          # Bot aus der Quote nehmen
/lp group admin permission set mrsleep.exempt false             # Admins mit * zählen trotzdem
/lp group mod permission set mrsleep.admin.gui true             # Mods: Menü …
/lp group mod permission set mrsleep.admin.ban true             # … und Sperren
/lp group mod permission set mrsleep.admin.clear true
/lp group vip permission set mrsleep.nosleep.nocooldown true    # VIPs ohne Cooldown
/lp group default permission set mrsleep.nosleep false          # Gäste ohne /nosleep
```

---

## Häufige Fragen
**Ich lege mich hin, aber die Nacht wird nicht übersprungen.**
`/mrsleep debug` oder das Admin-Menü zeigt für jeden Spieler, ob er zählt (und warum nicht), ob er schläft und ob eine Blockade aktiv ist. Häufigste Ursache: Über `*` hat der Spieler auch `mrsleep.exempt`.

**Der Cooldown greift nicht.**
Prüf, ob der Spieler `mrsleep.nosleep.nocooldown` oder einen Bypass hat. Das steht in `/mrsleep debug` und im Menü.

**Die Gamerule `playersSleepingPercentage` steht auf 100.**
Das ist gewollt. Die Quote kommt aus `sleep.percentage`.

**Was zählt als „eine Nacht“?**
Wer tagsüber `/nosleep` nutzt, blockiert die kommende Nacht, wer es nachts nutzt, die laufende. Blockaden gelten pro Welt.

---

## Selbst bauen & Releases
Voraussetzung ist JDK 25. Gradle kommt über den Wrapper.
```text
gradlew.bat build     (Windows)   ./gradlew build   (Linux/macOS)
```
Die JAR liegt danach in `build/libs/`. In VS Code reicht **Strg+Shift+B**.

**Branches:** Entwickelt wird auf **`beta`**, und Pull Requests bitte auch gegen `beta` stellen. **`main`** enthält nur fertige, stabile Versionen.

**Release** (GitHub und Modrinth):
1. In `gradle.properties` die `version` erhöhen.
2. In `CHANGELOG.md` einen Abschnitt `## [<version>]` anlegen.
3. Committen, dann den Tag erstellen und pushen: `git tag v<version>` und `git push origin v<version>`.
   Vorabversionen werden von `beta` getaggt, stabile Versionen von `main` (nach dem Merge von `beta`). Den genauen Ablauf beschreibt die [Projektdoku](docs/DOKUMENTATION.md#versionierung--release).

Alle `0.x`-Versionen und Versionen mit Suffix (z.B. `1.0.0-rc.1`) werden automatisch als **Vorabversion** veröffentlicht: auf Modrinth als „Beta“, auf GitHub als „Pre-release“.

Der Workflow `.github/workflows/release.yml` baut die JAR, erstellt das GitHub-Release mit dem Changelog-Abschnitt und lädt die Version auf Modrinth hoch. Für den Modrinth-Upload muss das Repository-Secret `MODRINTH_TOKEN` gesetzt sein.

Den Code-Aufbau beschreibt die [Projektdokumentation](docs/DOKUMENTATION.md).

---

## Lizenz
MRsleep ist **source-available**, aber nicht Open Source. Es gilt die [MRsleep Lizenz](LICENSE), die Kurzfassung:

| ✅ Erlaubt | ❌ Nicht erlaubt |
|---|---|
| Auf eigenen Servern nutzen, auch mit Spenden oder Rängen | Weiterveröffentlichen (auch verändert), z.B. auf Modrinth, Spigot, Hangar |
| Für den eigenen Gebrauch verändern und neu kompilieren | Verkaufen oder gegen Geld anbieten |
| An das eigene Server-Team oder den Hoster weitergeben | Als eigenes Werk ausgeben, Lizenzhinweis entfernen |
| Verbesserungen per Pull Request einreichen | |

Für alles andere ist eine schriftliche Erlaubnis nötig. Maßgeblich ist der vollständige Text in [LICENSE](LICENSE).
