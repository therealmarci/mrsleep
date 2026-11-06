# Admin menu & team limits

`/mrsleep gui` opens an overview that updates every second:
- **World:** time of day, how many are counted, sleeping and needed, who is blocking. A click switches to the next world.
- **Player heads:** blockers glow. The tooltip shows whether someone is counted (and why not), sleeping, blocking, has a cooldown or a ban.
- **Filter:** all, sleeping, blockers, exempt, banned.

## Player menu
A click on a head opens the **player menu**:
- Bypass for 30m / 2h / 1d (lift the cooldown)
- Reset cooldown
- Ban for 1h / 1d / permanently
- Lift block
- Teleport to the player

Every action needs its own [permission](permissions.md) - without it the button says "No permission". You set the durations and sounds under `gui` in the config (`bypass-durations`, `ban-durations`, `sounds`, `refresh-ticks`).

## How long the team may
`team-limits` in the config - applies to commands, the menu and the MRadmin web interface:

| | Bypass at most | Ban at most |
|---|---|---|
| Support (`mrsleep.admin.bypass` / `.ban`) | `team-limits.support.bypass` (default 30 min) | `team-limits.support.ban` (default 1 day) |
| Staff (`.bypass.staff` / `.ban.staff`) | `team-limits.staff.bypass` (default 1 day) | `team-limits.staff.ban` (default 1 day) |
| Admin (`.bypass.unlimited` / `.ban.unlimited`) | unlimited | unlimited, also permanent |

Buttons with a longer duration show "No permission", a command that is too long says how long you may at most. Bypass, bans and cooldown reset also work for players who are offline.

> [!TIP]
> With MRadmin the same is available in the browser: page **Sleep** in the web interface and the MRsleep section on every player page.
