# Display & chat messages

## Progress
Where "1/2 sleeping" or a block is shown is set by `sleep.display.mode`:

| Value | Display |
|---|---|
| `bossbar` (default) | Bar at the top of the screen - not covered by the chat while in bed |
| `actionbar` | Above the hotbar (covered by the chat window while in bed) |
| `title` | Subtitle in the middle of the screen |
| `none` | Nothing |

You set the colors of the boss bar with `sleep.display.bossbar-color` and `bossbar-blocked-color` (pink, blue, red, green, yellow, purple, white).

Minecraft's built-in "x/y players sleeping" counts all players and does not know MRsleep's rules. MRsleep therefore replaces it right away with its own progress (with `actionbar`) or hides it (`sleep.display.hide-vanilla-message`, on by default).

## Chat
- **Sleep message** "Alex went to bed (1/2)" to the world, the whole server or nobody: `sleep.chat.sleep-message` (`world` / `server` / `none`).
- **"Good morning!"** when the night was skipped: `sleep.chat.announce-skip`.
- Messages when the night is blocked, when a block expires and when an admin changes something.

Every message is in the language file and can be changed or turned off with `""` - see [Languages & messages](https://mrtechnology.marcelr.at/wiki/allgemein/sprachen/).
