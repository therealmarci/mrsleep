# Commands

## For players
| Command | What it does | Permission |
|---|---|---|
| `/nosleep [nights\|off]` | Block sleeping or lift your own block | `mrsleep.nosleep` |
| `/nosleep info` | Your own sleep and block status | `mrsleep.nosleep.info` |
| `/mrsleep version` | Version and update status | `mrsleep.version` (in `mrsleep.player`) |

## For the team
| Command | What it does | Permission |
|---|---|---|
| `/mrsleep gui` | Open the [admin menu](admin-menue.md) | `mrsleep.admin.gui` |
| `/mrsleep status [world]` | Short sleep status | `mrsleep.admin.status` |
| `/mrsleep debug` | Details per world and player - **first stop for problems** | `mrsleep.admin.status` |
| `/mrsleep clear [world]` | Remove all blocks of a world | `mrsleep.admin.clear` |
| `/mrsleep release <player>` | Lift a player's block | `mrsleep.admin.clear` |
| `/mrsleep bypass <player> <duration>` | Lift the cooldown for a while (`30m`, `2h`, `1d12h`) - at most as long as your [limit](admin-menue.md#how-long-the-team-may) | `mrsleep.admin.bypass` |
| `/mrsleep bypass remove <player>` · `bypass list` | Remove a bypass · show all | `mrsleep.admin.bypass` |
| `/mrsleep resetcooldown <player>` | Reset the cooldown right away | `mrsleep.admin.resetcooldown` |
| `/mrsleep ban <player> [duration\|permanent] [reason]` | Exclude from `/nosleep` (without a duration: permanently, needs `.ban.unlimited`) - at most as long as your [limit](admin-menue.md#how-long-the-team-may) | `mrsleep.admin.ban` |
| `/mrsleep unban <player>` · `bans` | Lift a ban · show all bans | `mrsleep.admin.ban` |
| `/mrsleep discord [test]` | Show the Discord status, send a test message | `mrsleep.admin.discord` |
| `/mrsleep reload` | Reload config and language file | `mrsleep.admin.reload` |

All commands have tab completion. Bypass, bans and cooldown reset also work for players who are offline.
