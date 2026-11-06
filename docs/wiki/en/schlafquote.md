# Sleep quota with exemptions

Players who are not counted are **taken out of the calculation completely**:

> Quota 50 %, 6 players online, 2 of them are bots with `mrsleep.exempt` → 4 players are counted → **2 have to sleep**.

The quota is set in `sleep.percentage` (default 50), at least `sleep.minimum-sleepers` players have to sleep (never more than are counted). Like in vanilla, the night is skipped when the required players have been in bed for about 5 seconds; with `sleep.clear-weather` rain or thunder ends as well.

## Who is not counted
| Who | Setting |
|---|---|
| Players with the permission `mrsleep.exempt` (e.g. bots and cam accounts) | - |
| Spectators | `sleep.ignore.spectators` |
| Players an AFK plugin marks as "sleeping ignored" | `sleep.ignore.sleeping-ignored` |
| Players in MRadmin's admin mode | `sleep.ignore.admin-mode` |
| Players in an MRpvp duel | `sleep.ignore.pvp` |
| Dummy players of MRchunks | always |

Whoever is not counted gets a message with the reason when going to bed. Admin mode and duels need MRadmin, see [Working with other plugins](zusammenarbeit.md).

## Worlds
All normal overworlds are managed. The Nether and the End are excluded, you exclude further worlds with `sleep.disabled-worlds`. Every world votes on its own, time and weather only change there - this also applies to additional Multiverse worlds.

<details>
<summary>Why is the game rule <code>playersSleepingPercentage</code> set to 100?</summary>

Vanilla cannot really take players out of the calculation - "ignored" players still count towards the total there. That is why MRsleep does the math itself: in managed worlds the game rule is `100`, the actual quota is in `sleep.percentage`. This is intended, please do not change it.
</details>

## /nosleep from the admin side
How players use `/nosleep` is described under [/nosleep](nosleep.md). You can set:

| Setting | Default | Effect |
|---|---|---|
| `nosleep.cooldown` | `15m` | Waiting time between two blocks; only starts with the blocked night. OPs have it too - except with `mrsleep.nosleep.nocooldown`. |
| `nosleep.max-nights` | `3` | At most this many nights at once (`/nosleep <number>`) |
| `nosleep.prevent-bed-enter` | `false` | `true`: in a blocked night nobody gets into bed at all (so no spawn point is set either) |
| `nosleep.release-on-quit` | `true` | The block ends when the player goes offline |
| `nosleep.broadcast` | `true` | The world learns when someone blocks or lifts a block |

Admins can lift a player's cooldown for a while (**bypass**) or **ban** players from `/nosleep` - see [Admin menu & team limits](admin-menue.md). Blocks, cooldowns, bypasses and bans survive a restart.
