# Schlafquote mit Ausnahmen

Spieler, die nicht zählen, werden **komplett aus der Rechnung genommen**:

> Quote 50 %, 6 Spieler online, 2 davon sind Bots mit `mrsleep.exempt` → es zählen 4 Spieler → **2 müssen schlafen**.

Die Quote steht in `sleep.percentage` (Standard 50), mindestens `sleep.minimum-sleepers` Spieler müssen schlafen (nie mehr, als gezählt werden). Wie in Vanilla wird die Nacht übersprungen, wenn die nötigen Spieler etwa 5 Sekunden im Bett liegen; mit `sleep.clear-weather` endet dabei auch Regen oder Gewitter.

## Wer nicht zählt
| Wer | Einstellung |
|---|---|
| Spieler mit der Permission `mrsleep.exempt` (z.B. Bots und Cam-Accounts) | - |
| Zuschauer | `sleep.ignore.spectators` |
| Spieler, die ein AFK-Plugin als „sleeping ignored“ markiert | `sleep.ignore.sleeping-ignored` |
| Spieler im Admin-Modus von MRadmin | `sleep.ignore.admin-mode` |
| Spieler im Duell bei MRpvp | `sleep.ignore.pvp` |
| Dummy-Spieler von MRchunks | immer |

Wer nicht zählt, bekommt beim Hinlegen einen Hinweis mit dem Grund. Admin-Modus und Duelle brauchen MRadmin, siehe [Zusammenarbeit mit anderen Plugins](zusammenarbeit.md).

## Welten
Verwaltet werden alle normalen Oberwelten. Nether und End sind ausgenommen, weitere Welten schließt du über `sleep.disabled-worlds` aus. Jede Welt stimmt für sich ab, Zeit und Wetter ändern sich nur dort - das gilt auch für zusätzliche Welten von Multiverse.

<details>
<summary>Warum steht die Gamerule <code>playersSleepingPercentage</code> auf 100?</summary>

Vanilla kann Spieler nicht wirklich aus der Rechnung nehmen - „ignorierte“ Spieler zählen dort weiter zur Gesamtzahl. MRsleep rechnet deshalb selbst: In verwalteten Welten steht die Gamerule auf `100`, die eigentliche Quote steht in `sleep.percentage`. Das ist gewollt, bitte nicht ändern.
</details>

## /nosleep aus Admin-Sicht
Wie Spieler `/nosleep` nutzen, steht unter [/nosleep](nosleep.md). Einstellen lässt sich:

| Einstellung | Standard | Wirkung |
|---|---|---|
| `nosleep.cooldown` | `15m` | Wartezeit zwischen zwei Blockaden; startet erst mit der blockierten Nacht. Auch OPs haben ihn - außer mit `mrsleep.nosleep.nocooldown`. |
| `nosleep.max-nights` | `3` | Höchstens so viele Nächte auf einmal (`/nosleep <zahl>`) |
| `nosleep.prevent-bed-enter` | `false` | `true`: In einer blockierten Nacht kommt man gar nicht ins Bett (setzt also auch keinen Spawnpunkt) |
| `nosleep.release-on-quit` | `true` | Blockade endet, wenn der Spieler offline geht |
| `nosleep.broadcast` | `true` | Die Welt erfährt, wenn jemand blockiert oder aufhebt |

Admins können den Cooldown eines Spielers auf Zeit aufheben (**Bypass**) oder Spieler von `/nosleep` **sperren** - siehe [Admin-Menü & Team-Grenzen](admin-menue.md). Blockaden, Cooldowns, Bypässe und Sperren überstehen einen Neustart.
