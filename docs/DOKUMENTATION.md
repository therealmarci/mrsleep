# MRsleep – Projektdokumentation

Diese Dokumentation richtet sich an alle, die am Code von MRsleep arbeiten. Was das Plugin für Server-Admins kann und wie man es einrichtet, steht in der [README](../README.md).

**Inhalt:**
[1. Überblick](#1-überblick) ·
[2. Projektstruktur](#2-projektstruktur) ·
[3. Architektur](#3-architektur) ·
[4. Schlaflogik](#4-schlaflogik-sleepmanager) ·
[5. /nosleep](#5-nosleep-nosleepmanager-nosleepcommand) ·
[6. Config](#6-konfiguration-laden--migrieren-configupdater) ·
[7. Admin-Menü](#7-admin-menü-gui) ·
[8. Update-Checker](#8-update-checker-updatechecker) ·
[9. Nachrichten](#9-nachrichten-messages) ·
[10. Discord](#10-discord-webhook-discordwebhook) ·
[11. Permissions](#11-permissions-design) ·
[12. Entwicklung](#12-entwicklung) ·
[13. Ideen](#13-erweiterungsideen)

---

## 1. Überblick

| | |
|---|---|
| Plattform | Paper 26.3 (`io.papermc.paper:paper-api:26.3.build.200-stable`) |
| Java | 25 (Gradle-Toolchain, `options.release = 25`) |
| Build | Gradle 9.8.1 über den Wrapper (`gradlew`) |
| Plugin-Typ | Klassisches Bukkit-Plugin (`plugin.yml`, `JavaPlugin`) |
| Externe Abhängigkeiten | keine. Adventure/MiniMessage, Gson und `java.net.http` sind in Paper bzw. im JDK enthalten |
| Paket | `at.marcelr.mrsleep` |

## 2. Projektstruktur

```text
mrsleep/
├─ build.gradle.kts            Build, Paper-API, Java 25, Ressourcen-Ersetzung, Modrinth-Upload (Minotaur)
├─ gradle.properties           version, modrinthProjectId, githubRepository, supportedGameVersions
├─ settings.gradle.kts
├─ gradlew / gradlew.bat       Gradle-Wrapper (lädt Gradle 9.8.1 selbst)
├─ CHANGELOG.md                Änderungen pro Version (Quelle für Release-Notes)
├─ LICENSE                     MRsleep Lizenz (source-available)
├─ .editorconfig               Formatierung; hält das BOM der config.yml
├─ .github/
│  ├─ workflows/build.yml      Build + Tests bei jedem Push/PR, JAR als Artefakt
│  ├─ workflows/release.yml    Bei Tag v*: GitHub-Release + Modrinth-Upload
│  ├─ ISSUE_TEMPLATE/          Vorlagen für Fehlermeldungen und Wünsche
│  └─ dependabot.yml           monatliche Updates der GitHub-Actions
├─ docs/DOKUMENTATION.md       diese Datei
├─ README.md                   Doku für Server-Admins
└─ src/
   ├─ main/java/at/marcelr/mrsleep/
   │  ├─ MRSleep.java              Hauptklasse: Lebenszyklus, verbindet alle Komponenten
   │  ├─ Settings.java             unveränderlicher Record mit allen Werten aus config.yml
   │  ├─ ConfigUpdater.java        Config-Migration, Laden mit BOM-Unterstützung
   │  ├─ Messages.java             MiniMessage (Chat, Broadcast, Item-Namen/Lore)
   │  ├─ StatusTexts.java          Status-Texte eines Spielers (für GUI und /nosleep info)
   │  ├─ SleepDisplay.java         Fortschrittsanzeige: Bossbar / Actionbar / Titel
   │  ├─ SleepManager.java         eigene Schlaflogik (Quote, Nacht überspringen)
   │  ├─ NoSleepManager.java       Blockaden, Cooldowns, Bypässe, Sperren, data.yml
   │  ├─ AdminActions.java         Admin-Aktionen, gemeinsam für Befehle und Menü
   │  ├─ DiscordWebhook.java       asynchrone Discord-Logs
   │  ├─ UpdateChecker.java        Update-Prüfung (Standard: GitHub Releases, alternativ Modrinth)
   │  ├─ SleepListener.java        Events (Bett, Join/Quit, TimeSkip, WorldLoad)
   │  ├─ command/
   │  │  ├─ NoSleepCommand.java    /nosleep [nächte|off|info]
   │  │  └─ MRSleepCommand.java    /mrsleep …
   │  ├─ gui/
   │  │  ├─ Menu.java              Basisklasse (InventoryHolder, Klick-Aktionen, render())
   │  │  ├─ MenuListener.java      Klick-Routing, Item-Schutz, Live-Aktualisierung
   │  │  ├─ MainMenu.java          Übersicht (Welt, Köpfe, Filter, Seiten)
   │  │  ├─ PlayerMenu.java        Aktionen für einen Spieler
   │  │  ├─ PlayerHead.java        Kopf mit Status-Lore, Sortierung
   │  │  └─ Items.java             Item-Builder
   │  └─ util/
   │     ├─ DurationUtil.java      "1h30m" ⇄ Millisekunden
   │     └─ Versions.java          SemVer-Vergleich, Erkennung von Vorabversionen
   ├─ main/resources/
   │  ├─ plugin.yml                Befehle und Permissions (${version} wird ersetzt)
   │  ├─ config.yml                Standard-Config (UTF-8 mit BOM!)
   │  └─ build-info.properties     Version und Projekt-IDs für den Update-Checker (wird ersetzt)
   └─ test/java/…/util/            JUnit-Tests für DurationUtil und Versions
```

## 3. Architektur

```text
                         ┌──────────────┐
                         │   MRSleep    │  onEnable / onDisable / reload
                         └──────┬───────┘
   ┌─────────┬─────────┬────────┼─────────┬──────────────┬──────────────┐
   ▼         ▼         ▼        ▼         ▼              ▼              ▼
Settings  Messages  Discord  Update-  NoSleepManager  SleepManager   MenuListener
                    Webhook  Checker    (Zustand)      (Logik)       (GUI-Refresh)
                                            ▲              ▲
                                     AdminActions ◄────────┤
                                       ▲      ▲            │
                              MRSleepCommand  MainMenu/PlayerMenu   SleepListener, NoSleepCommand
```

- **`MRSleep`** besitzt alle Komponenten und reicht sie über Getter weiter (`plugin.settings()`, `plugin.noSleep()` …). Es gibt keine statischen Singletons.
- **`Settings`** ist ein Record. Bei `reload()` wird er komplett neu erzeugt, sodass niemand einen halb aktualisierten Zustand sieht.
- **Zustand** (Blockaden, Cooldowns, Bypässe, Sperren) liegt nur im `NoSleepManager`. **Logik** über die Schlafquote liegt nur im `SleepManager`.
- **`AdminActions`** kapselt jede Admin-Aktion mit Rückmeldung, Benachrichtigung des Spielers, Broadcast und Discord-Log. Befehle und Menü rufen dieselben Methoden auf.
- Alles läuft auf dem **Main-Thread**, mit zwei Ausnahmen: HTTP im `DiscordWebhook` (eigener Daemon-Thread) und im `UpdateChecker` (Bukkit-Async-Task). Ergebnisse, die Spieler betreffen, gehen per `runTask` zurück auf den Main-Thread.

## 4. Schlaflogik (`SleepManager`)

### Warum eine eigene Logik?
In Vanilla bzw. Paper zählen Spieler mit `setSleepingIgnored(true)` als schlafend, bleiben aber in der Gesamtzahl. Bei 6 Spielern, 2 ignorierten und 50 % braucht Vanilla `ceil(6 × 0.5) = 3` Schlafende, von denen 2 die ignorierten sind. Es reicht also **1** echter Schläfer. Gewünscht sind aber **2 von 4**. Dafür gibt es keinen API-Hook, deshalb:

1. **Vanilla zurückdrängen:** In verwalteten Welten wird `GameRules.PLAYERS_SLEEPING_PERCENTAGE = 100` gesetzt. Vanilla versucht dann nur zu überspringen, wenn wirklich alle schlafen, und dieser Versuch wird abgebrochen (Punkt 2).
   > ⚠️ **Nicht 101 verwenden:** Bei Werten über 100 gibt `ServerLevel#canSleepThroughNights()` `false` zurück. `ServerPlayer#startSleepInBed` schickt dann jedem Spieler beim Hinlegen „Keine noch so lange Rast kann diese Nacht verkürzen“ (`sleep.not_possible`), und die Spieler stehen wieder auf. Das war der Fehler bis Version 0.9.0.
   > Nebeneffekt von 100: Liegen in einer `/nosleep`-Nacht wirklich alle im Bett, beendet Vanilla trotzdem Regen und Gewitter (`resetWeatherCycle` wird unabhängig vom abgebrochenen Event aufgerufen).
2. **Sicherheitsnetz:** `SleepListener#onTimeSkip` bricht jedes `TimeSkipEvent` mit `SkipReason.NIGHT_SKIP` in verwalteten Welten ab.
3. **Eigener Check:** Ein Task prüft alle 10 Ticks jede verwaltete Welt.

### Verwaltete Welten
`isManaged(world)` gilt für `Environment.NORMAL`, sofern die Welt nicht in `sleep.disabled-worlds` steht. In `managedWorlds` merkt sich das Plugin, wo es die Gamerule geändert hat. So kann es sie beim Deaktivieren oder beim Entfernen einer Welt aus der Verwaltung (Reload) wieder auf `sleep.percentage` setzen.

### Ablauf pro Tick und Welt
```text
housekeeping(world)
  ├─ abgelaufene Blockaden entfernen → Chat "nosleep-expired" + Discord
  └─ Nacht hat begonnen (einmal pro nightKey) und blockiert → Chat/Discord "night-start-blocked"
checkSleep(world)
  ├─ status(world): counted / exempt / sleeping / deepSleeping / needed
  ├─ niemand schläft → fertig
  ├─ blockiert → SleepDisplay#blocked ("display-blocked", nur Schlafende) → fertig
  ├─ deepSleeping ≥ needed → skipNight()
  └─ sonst SleepDisplay#progress ("display-sleeping", alle Spieler der Welt)
  (niemand schläft oder Nacht übersprungen → SleepDisplay#hide)
```

### Berechnung
```text
excluded  = hasPermission("mrsleep.exempt")
          ∨ (ignore-spectators ∧ Zuschauermodus)
          ∨ (respect-sleeping-ignored ∧ isSleepingIgnored())
counted   = Spieler der Welt, die nicht ausgeschlossen sind
needed    = clamp( max(minimum-sleepers, ceil(counted × percentage / 100)), 1, counted )
deep      = schlafende gezählte Spieler mit getSleepTicks() ≥ 100   (wie Vanilla)
skip, wenn counted > 0 ∧ deep ≥ needed ∧ keine aktive Blockade
```
Ausgeschlossene Spieler zählen weder zu `counted` noch zu `sleeping`, auch wenn sie im Bett liegen.

### `skipNight`
- `setFullTime(fullTime + (24000 − time % 24000))` springt auf den nächsten Morgen (Tick 0).
- Alle Schlafenden werden mit `wakeup(false)` geweckt.
- Mit `clear-weather` werden `setStorm(false)` und `setThundering(false)` gesetzt.
- Danach folgen die Chat-Nachricht `night-skipped` und das Discord-Event `night-skipped`.

## 5. /nosleep (`NoSleepManager`, `NoSleepCommand`)

### Nacht-Schlüssel
```java
nightKey = fullTime / 24000 (+1, wenn time ≥ 23460)
```
Eine Nacht gehört zu dem Ingame-Tag, an dessen Abend sie beginnt. Tagsüber zeigt der Schlüssel deshalb auf die kommende Nacht, nachts auf die laufende. Ab 23460 funktionieren Betten nicht mehr, die Nacht ist vorbei und der Schlüssel springt auf den nächsten Tag.

| Konstante | Wert | Bedeutung |
|---|---|---|
| `NIGHT_START` | 12542 | Ab hier kann man bei klarem Wetter schlafen |
| `NIGHT_END` | 23460 | Ab hier kann man nicht mehr schlafen |

### Blockaden
Gespeichert als `world → (player → Block(startKey, endKey))`, mit erhaltener Einfügereihenfolge für die Namensliste.

- `/nosleep N` setzt `Block(key, key + N − 1)` und ersetzt eine bestehende eigene Blockade.
- Aktiv ist ein Block, solange `currentKey ≤ endKey`.
- Verbleibende Nächte: `min(endKey − currentKey + 1, endKey − startKey + 1)`. Die zweite Grenze verhindert zu große Werte, wenn jemand die Zeit per `/time set` zurückdreht.
- Zeigt das Plugin eine einzige Zahl an („noch X Nächte“), ist das die **längste** aktive Blockade (`NoSleepManager.longest`), also die Zahl der Nächte, bis wieder geschlafen werden kann.
- Wird die Nacht anders übersprungen, z.B. per `/time set day`, läuft die Blockade beim nächsten Schlüsselwechsel normal ab.

### Befehlslogik
```text
/nosleep off|aus|cancel  → eigene Blockade aufheben (kein Cooldown)
/nosleep  (aktiv)        → eigene Blockade aufheben (kein Cooldown)
/nosleep  (nicht aktiv)  → 1 Nacht blockieren            ┐ prüft Cooldown; startet ihn sofort, wenn
/nosleep N               → N Nächte (1..max-nights)      ┘ man schlafen kann, sonst bei Nachtbeginn
```
**Cooldown erst bei Nachtbeginn:**
- Jeder `Block` hat ein Flag `charged`. `setBlock` setzt es sofort (und ruft `markUsed` auf), wenn `SleepManager.sleepPossible(world)` gilt (`isNight` oder Gewitter). Sonst bleibt der Block „pending“.
- `SleepManager#housekeeping` ruft alle 10 Ticks `NoSleepManager#chargePending` auf. Sobald man in der Welt schlafen kann, startet bei allen wartenden Blöcken der Cooldown (`lastUse = jetzt`), und die Spieler bekommen `nosleep-cooldown-started`.
- Hebt der Spieler einen wartenden Block selbst auf (`NoSleepCommand#lift`), wurde nie ein Cooldown gesetzt (`nosleep-deactivated-free`). Bei einem schon belasteten Block läuft der Cooldown weiter (`nosleep-deactivated-cooldown`).
- Blöcke aus `data.yml` vor 0.9.4 haben kein Flag und gelten als belastet.

Der Cooldown entfällt bei `mrsleep.nosleep.nocooldown` (`default: false`, gilt also auch nicht automatisch für OPs) oder bei einem aktiven Bypass (`bypassUntil > now`). Vor der Cooldown-Prüfung wird geprüft, ob eine **Sperre** besteht. Das Aufheben einer eigenen Blockade bleibt auch gesperrten Spielern möglich.

### Sperren
`NoSleepManager.Ban(until, reason, by)`, wobei `until == Long.MAX_VALUE` dauerhaft bedeutet. `AdminActions#ban` speichert die Sperre und hebt sofort alle laufenden Blockaden des Spielers auf. Abgelaufene Sperren werden beim Zugriff entfernt.

### Persistenz: `plugins/MRsleep/data.yml`
```yaml
cooldowns:
  <spieler-uuid>: <zeitpunkt letzte Nutzung, epoch ms>
bypass:
  <spieler-uuid>: <ablaufzeitpunkt, epoch ms>
bans:
  <spieler-uuid>:
    until: <epoch ms | 9223372036854775807 = dauerhaft>
    reason: <text>
    by: <name des admins>
blocks:
  <welt-uuid>:
    <spieler-uuid>:
      start: <nightKey>
      end: <nightKey>
```
Die Datei wird nach jeder Änderung synchron geschrieben (sie ist sehr klein) und zusätzlich in `onDisable`. Abgelaufene Einträge werden beim Speichern entfernt.

## 6. Konfiguration laden & migrieren (`ConfigUpdater`)
- **Laden:** `MRSleep#getConfig()` und `reloadConfig()` sind überschrieben und nutzen `ConfigUpdater.load()`. Dort wird ein UTF-8-**BOM** am Dateianfang entfernt, bevor SnakeYAML die Datei liest. Als Defaults dient die mitgelieferte `config.yml`, fehlende Schlüssel greifen also auf sie zurück.
- **BOM:** Die mitgelieferte `config.yml` beginnt mit `EF BB BF`. Daran erkennen Windows-Editoren und FTP-Clients die Kodierung, statt Windows-1252 zu raten. Sonst werden Umlaute zu „Ã¤“.
  ⚠️ Beim Bearbeiten von `src/main/resources/config.yml` darauf achten, dass das BOM erhalten bleibt (in VS Code unten rechts „UTF-8 with BOM“).
- **Migration:** `config-version` in der Datei kleiner als `ConfigUpdater.CURRENT_VERSION`:
  1. Die mitgelieferte `config.yml` wird als Vorlage geladen. Die Kommentare und die Reihenfolge bleiben erhalten, weil Bukkit Kommentare mitliest.
  2. Jeder Wert der alten Datei, dessen Schlüssel es in der Vorlage noch gibt, wird übernommen. Ausnahme sind die `REPLACED_KEYS`, also Texte, deren Inhalt sich mit neuen Funktionen geändert hat.
  3. Die alte Datei wird als `config.v<N>.backup.yml` gesichert, die neue mit BOM geschrieben.
- **Wenn sich die Config-Struktur ändert:** `CURRENT_VERSION` und `config-version` in der Vorlage erhöhen. Bei umbenannten Schlüsseln die Werte vor dem Übernehmen umschreiben, bei geänderten Standardtexten den Schlüssel in `REPLACED_KEYS` aufnehmen.

## 7. Admin-Menü (`gui/*`)
- **`Menu`** ist selbst der `InventoryHolder`. `MenuListener` erkennt Menüs über `getTopInventory().getHolder(false) instanceof Menu`. Jeder Klick in ein Menü wird abgebrochen, ebenso jeder Drag, damit sich keine Items herausnehmen lassen. Danach wird die Aktion des angeklickten Slots ausgeführt.
- **`render()`** leert das Inventar und ruft `draw()` neu auf. `MenuListener` macht das alle `gui.refresh-ticks` Ticks für jedes offene Menü. Da Bukkit Inventaränderungen erst am Tick-Ende synchronisiert, flackert dabei nichts.
- **`MainMenu`**: Oben die Welt (eine Uhr, nachts ein Recovery-Kompass, Klick wechselt die Welt), das Info-Buch (Version und Update-Status) und „Blockaden entfernen“. In der Mitte bis zu 36 Köpfe pro Seite, sortiert nach `PlayerHead.rank` (Blockierer → Schlafende → Zählende → Ausgenommene, danach alphabetisch) und gefiltert nach `Filter`. Unten Seiten, Filter, Schließen und Reload.
- **`PlayerMenu`**: Aktionen für einen Spieler. Jede Taste läuft über `button(...)`: Sie prüft die Permission (sonst Lore-Hinweis, Ablehnungs-Sound und `no-permission`), ruft die Methode in `AdminActions` auf, spielt den Erfolgs-Sound und zeichnet neu. Geht der Spieler offline, springt das Menü zur Übersicht zurück.
- Alle Texte kommen aus `messages.gui-*`. `Messages#item` und `Messages#lore` entfernen die Kursivschrift, die Minecraft standardmäßig für Item-Texte nutzt. Werte in der Kopf-Lore (z.B. `<counts>`) sind formatierte Komponenten (`Messages.tag(name, Component)`).

## 8. Update-Checker (`UpdateChecker`)
- Die Projekt-IDs stehen in `gradle.properties` und landen beim Build in `build-info.properties`.
- **Modrinth:** `GET /v2/project/{id}/version?loaders=["paper"]&include_changelog=false`. **GitHub:** `GET /repos/{repo}/releases`. Entwürfe werden ignoriert. Vorabversionen werden berücksichtigt, wenn `include-prereleases` an ist oder die installierte Version selbst eine Vorabversion ist (`Versions.isPrerelease`: 0.x oder Suffix).
- Die höchste Version wird mit `Versions.compare` bestimmt (SemVer): Zahlen werden numerisch verglichen (`0.10.0 > 0.9.2`), ein führendes `v` und Build-Metadaten (`+…`) werden ignoriert, eine Release-Version gilt als neuer als ihre Vorabversion (`1.0.0 > 1.0.0-rc.1`), und Vorabversionen werden je Teil verglichen (`rc.10 > rc.2`).
- Der Check läuft beim Start (nach 5 s) und danach alle `interval-hours` Stunden als Async-Task, außerdem bei `/mrsleep version`. Ist eine neuere Version verfügbar, erscheint einmal pro Version ein Hinweisblock in der Konsole, und online-Admins (`mrsleep.admin.update`) bekommen eine Nachricht. Beim Join kommt der Hinweis nach 3 s.
- Fehler wie 404 (Projekt noch nicht veröffentlicht) werden einmal als INFO geloggt, danach erst wieder, wenn sich der Grund ändert.
- Der User-Agent lautet `MRsleep/<version> (<github-url>)`. Modrinth verlangt einen eindeutigen User-Agent.

## 9. Nachrichten (`Messages`)
- Alle Texte stehen unter `messages.*` in der Config und werden mit **MiniMessage** dargestellt.
- `<prefix>` wird als *parsed* Placeholder eingesetzt, darf also selbst Formatierung enthalten. Alle anderen Platzhalter sind *unparsed*, damit Spielernamen keine Tags einschleusen können.
- Eine leere Nachricht (`""`) wird nicht gesendet. Damit lässt sich jeder Hinweis abschalten. Ein **fehlender** Schlüssel erzeugt sichtbar `Fehlende Nachricht: <key>`.
- `nights(n)` erzeugt mit `nights-singular` und `nights-plural` z.B. „1 Nacht“ oder „3 Nächte“.
- `broadcast(world, …)` sendet nur an Spieler **dieser Welt**, denn Blockaden gelten pro Welt.

Wo welche Nachricht ausgelöst wird:

| Schlüssel | Auslöser |
|---|---|
| `bed-prevented` / `bed-blocked-warning` | `SleepListener#onBedEnter` (je nach `prevent-bed-enter`) |
| `player-sleeping` | `SleepListener#onBedEntered`, einen Tick nach dem Hinlegen, nur wenn nicht blockiert und der Spieler zählt |
| `join-blocked-info` | `SleepListener#onJoin` |
| `nosleep-broadcast-quit` | `SleepListener#onQuit` |
| `night-start-blocked`, `nosleep-expired` | `SleepManager#housekeeping` |
| `display-*` | `SleepDisplay` (über `SleepManager#checkSleep`) |
| `night-skipped` | `SleepManager#skipNight` |
| `player-sleeping` (Empfänger) | `SleepListener#onBedEntered`, je nach `sleep.sleep-message` |
| `nosleep-*` | `NoSleepCommand` |
| `bypass-*`, `cooldown-reset*`, `cleared*`, `released`, `ban-*`, `unban-*`, `reloaded` | `AdminActions` (Befehle und Menü) |
| `status`, `debug-*`, `bans-*`, `version*` | `MRSleepCommand` |
| `update-*` | `UpdateChecker` |
| `gui-*` | `gui/*` |

## 10. Discord-Webhook (`DiscordWebhook`)
- `load(section)` liest `discord.*` und prüft die URL. Ist sie leer oder ungültig, bleibt Discord aus. Der Grund wird in der Konsole gewarnt und in `disabledReason` gehalten. Bei Erfolg steht „Discord-Webhook aktiv (x/y Events eingeschaltet)“ in der Konsole.
- **Events:** Die Einträge aus `discord.events` der eigenen Datei, ergänzt um alle Events aus der mitgelieferten Config, die dort fehlen. Bukkit würde für fehlende Abschnitte sonst stillschweigend leere Sektionen liefern. Ruft der Code ein Event auf, das es trotzdem nicht gibt, kommt einmalig eine Warnung.
- `log(eventKey, key1, value1, key2, value2, …)` ersetzt `{key}` im Event-Text. Die Werte werden für Discord-Markdown escaped (`_ * ~ \` | > \\`), damit z.B. `Mr_Steve` nicht kursiv wird.
- Das JSON entsteht mit Gson: ein Embed mit `description`, `color`, `timestamp` und optional `footer`, oder einfacher `content`. Zusätzlich wird immer `allowed_mentions: {parse: []}` gesetzt, damit nie jemand gepingt wird.
- Gesendet wird über `java.net.http.HttpClient` auf **einem** Single-Thread-Executor. Dadurch bleibt die Reihenfolge erhalten, und bei HTTP 429 kann der Thread `retry_after` abwarten (höchstens 3 Versuche).
- Der Zustand (`Config`-Record) ist `volatile` und wird beim Reload atomar ersetzt.
- `shutdown()` wartet in `onDisable` bis zu 5 Sekunden, damit letzte Logs noch rausgehen.
- **Diagnose:** `send()` gibt `null` (Erfolg) oder eine Fehlerbeschreibung zurück und merkt sich das Ergebnis in `lastResult`. Bekannte Statuscodes werden erklärt (400 abgelehnt, 401/403 Token ungültig, 404 Webhook gelöscht). `/mrsleep discord test` nutzt `test()`, das mit `?wait=true` sendet, damit Discord die Zustellung bestätigt.
- `bed-blocked` wird über `SleepListener#loggedBedAttempts` pro Welt, Spieler und Nacht nur einmal gesendet. Das Set wird bei jedem `TimeSkipEvent` geleert.

## 11. Permissions-Design
- `mrsleep.exempt` hat `default: false`, damit OPs nicht automatisch aus der Quote fallen.
- `mrsleep.admin` enthält alle `mrsleep.admin.*` als Kinder, aber **nicht** `mrsleep.exempt`.
- Bei `/mrsleep` steht in der `plugin.yml` bewusst keine Permission. Sonst würde ein Moderator, der nur `mrsleep.admin.clear` hat, den Befehl gar nicht sehen. Die Prüfung passiert pro Unterbefehl in `MRSleepCommand#check`.
- Bekannte Einschränkung: Die LuckPerms-Wildcards `*` und `mrsleep.*` schließen `mrsleep.exempt` mit ein. Abhilfe steht in der README.

## 12. Entwicklung

### Voraussetzungen (bereits eingerichtet)
- Eclipse Temurin **JDK 25** (`C:\Program Files\Eclipse Adoptium\jdk-25…`, `JAVA_HOME` ist systemweit gesetzt)
- VS Code: *Extension Pack for Java* und *Gradle for Java*
- `.vscode/settings.json` (im Workspace-Root `c:\REPO\mc`) legt das JDK für Java- und Gradle-Extension fest und aktiviert verschachtelte Gradle-Projekte
- `.vscode/tasks.json`: **„MRsleep: Build“** (Standard-Build-Task, `Strg+Shift+B`) und **„MRsleep: Clean Build“**

### Bauen
```text
cd mrsleep
gradlew.bat build
→ build/libs/MRsleep-<version>.jar
```
Die Version steht in `gradle.properties` und wird beim Build in die `plugin.yml` (`${version}`) und in die `build-info.properties` übernommen.

### Tests
```text
gradlew.bat test        nur die Tests
gradlew.bat build       Kompilieren + Tests + JAR (schlägt fehl, wenn ein Test fehlschlägt)
```
- JUnit 6 (`src/test/java`). Getestet wird die Logik, die ohne laufenden Server auskommt: `DurationUtil` (Parsen und Formatieren von Zeitangaben) und `Versions` (SemVer-Vergleich, Vorabversionen).
- Alles, was Bukkit braucht (Schlafen, Events, GUI), prüft die manuelle Testcheckliste weiter unten auf dem Testserver.
- Die Ergebnisse stehen in `build/reports/tests/test/index.html`. Auf GitHub laufen die Tests bei jedem Push (`build.yml`) und vor jedem Release.
- **Neue Logik ohne Server-Bezug** am besten nach `util/` legen und dort testen.

### Versionierung & Release
- **Semantic Versioning:** `MAJOR.MINOR.PATCH`. **Vorabversionen** sind alle `0.x` (aktuelle Testphase) sowie Versionen mit Suffix wie `1.0.0-rc.1`. Sie landen auf Modrinth als „beta“ und auf GitHub als Pre-Release (`isPrerelease` in `build.gradle.kts`, `PRERELEASE` in `release.yml`). Ab `1.0.0` werden Versionen als normale Releases veröffentlicht.
- **Branches:**

  | Branch | Zweck | Tags / Releases |
  |---|---|---|
  | `beta` | Entwicklung. Hier wird gearbeitet, Pull Requests und Dependabot zielen hierhin. | Vorabversionen (`1.0.0-rc.1`, `1.1.0-beta.2`) |
  | `main` | Stabiler Stand. Bekommt nur Merges aus `beta`, wenn eine Version fertig ist. | Stabile Versionen (`1.0.0`, `1.0.1`) |

  Beide Branches werden bei jedem Push gebaut und getestet (`build.yml`). `release.yml` bricht ab, wenn eine **stabile** Version nicht auf einem Commit aus `main` getaggt wurde (`git merge-base --is-ancestor`).

- **Vorabversion (von `beta`):**
  1. Auf `beta`: `version` in `gradle.properties` setzen (z.B. `1.1.0-rc.1`).
  2. In `CHANGELOG.md` einen Abschnitt `## [<version>] - <datum>` anlegen. Dorthin kommt, was bisher unter „Unveröffentlicht“ stand.
  3. Committen, pushen, dann `git tag v<version>` und `git push origin v<version>`.
- **Stabile Version (von `main`):**
  1. Auf `beta`: `version` auf die finale Nummer setzen (z.B. `1.1.0`) und den Changelog-Abschnitt anlegen, dann committen und pushen.
  2. `git checkout main`, `git merge --no-ff beta`, `git push`.
  3. `git tag v<version>` und `git push origin v<version>`.
  4. Wieder `git checkout beta`, dann die nächste Version vorbereiten.
- **Hotfix für eine stabile Version:** Auf `main` beheben, als PATCH-Version taggen (z.B. `1.0.1`) und `main` anschließend nach `beta` mergen, damit der Fix nicht verloren geht.
- **`release.yml`** prüft, dass Tag und `gradle.properties` übereinstimmen, baut die JAR, liest den passenden Abschnitt aus `CHANGELOG.md` und erstellt damit das GitHub-Release mit der JAR. Ist das Repository-Secret `MODRINTH_TOKEN` gesetzt, folgt `./gradlew modrinth` (Minotaur). Der Changelog-Text kommt über die Umgebungsvariable `CHANGELOG`.
- **Einmalig einrichten:**
  - GitHub-Repository anlegen (Name wie `githubRepository` in `gradle.properties`) und pushen.
  - Auf Modrinth ein Projekt mit der Slug aus `modrinthProjectId` anlegen.
  - Auf Modrinth unter *Settings → PATs* ein Token mit dem Recht „Create versions“ erzeugen und auf GitHub unter *Settings → Secrets and variables → Actions* als `MODRINTH_TOKEN` hinterlegen.

### Manuelle Testcheckliste
| # | Test | Erwartung |
|---|---|---|
| 1 | Start, `/gamerule players_sleeping_percentage` | `100` in der Oberwelt |
| 1b | Hinlegen | **keine** Meldung „Keine noch so lange Rast …“, sofort Bossbar `1/x Spieler schlafen` mit Fortschrittsbalken, auch bei offenem Bett-Chat sichtbar |
| 1c | Spieler mit `mrsleep.exempt` legt sich hin | Hinweis `bed-excluded` mit Grund |
| 1d | `/mrsleep debug` | Pro Welt Gamerule/Zeit/Blockade, pro Spieler „zählt“ und „schläft (Ticks)“ |
| 2 | 4 Spieler, 1 mit `mrsleep.exempt`, 50 % | 2 von 3 müssen schlafen; Bossbar zeigt `x/2` |
| 3 | Ausgenommener Spieler schläft allein | Nacht wird **nicht** übersprungen |
| 4 | `/nosleep` tagsüber, dann nachts hinlegen | `bed-blocked-warning`, kein Skip |
| 5 | `/nosleep` erneut | Blockade aufgehoben, kein neuer Cooldown |
| 6 | `/nosleep 3`, dann 3 Nächte per `/time set` durchspielen | „noch 3/2/1 Nächte“, danach `nosleep-expired` |
| 7 | `/nosleep 4` bei `max-nights: 3` | `nosleep-invalid-nights` |
| 8 | Nachts `/nosleep`, aufheben, erneut `/nosleep` | `nosleep-deactivated-cooldown`, danach Cooldown-Meldung |
| 8b | Tagsüber `/nosleep` (Hinweis `nosleep-pending-info`), vor Nachtbeginn aufheben, erneut `/nosleep` | `nosleep-deactivated-free`, erneutes Blockieren klappt sofort |
| 8c | Tagsüber `/nosleep`, dann `/time set 13000` | `nosleep-cooldown-started`; `/mrsleep debug` zeigt vorher „startet bei Nachtbeginn“, danach „noch 15m“ |
| 9 | `/mrsleep bypass <spieler> 10m`, dann wie 8 | kein Cooldown, Spieler wird benachrichtigt |
| 10 | Blockierender Spieler geht offline | `nosleep-broadcast-quit`, Schlafen wieder möglich |
| 11 | Discord-URL eintragen, `/mrsleep reload` | Event `admin-reload` erscheint im Kanal |
| 12 | Plugin entfernen und Server neu starten | Gamerule steht wieder auf `sleep.percentage` |
| 13 | Alte Config (ohne `config-version`) und Start mit 0.9.2 | Log „config.yml von Version 1 auf 2 aktualisiert“, Werte übernommen, `config.v1.backup.yml` vorhanden, Datei beginnt mit BOM |
| 14 | `/mrsleep ban <spieler> 2h Test`, dann `/nosleep` als dieser Spieler | `nosleep-banned`, die laufende Blockade wurde aufgehoben |
| 15 | `/mrsleep gui` | Köpfe aktualisieren sich live (Schlafsekunden zählen hoch), Filter und Seiten funktionieren, Items lassen sich nicht herausnehmen |
| 16 | Spieler-Menü ohne `mrsleep.admin.ban` | Sperr-Buttons zeigen „Keine Berechtigung“, ein Klick wird abgelehnt |
| 17 | `/mrsleep version` | Version, Links und Update-Status (vor dem ersten Release: „Projekt nicht gefunden“) |

## 13. Erweiterungsideen
- Eine Permission pro Nächtezahl (z.B. `mrsleep.nosleep.nights.5`), um `max-nights` je Gruppe zu staffeln
- PlaceholderAPI-Expansion (`%mrsleep_needed%`, `%mrsleep_blocked%`)
- Sanftes Vorspulen der Zeit statt eines Sprungs
- Ein eigener Cooldown für das Aufheben, falls `/nosleep` als Spam-Werkzeug missbraucht wird
