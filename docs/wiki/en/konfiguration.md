# Configuration

Easiest in the browser: with **MRadmin** under **Plugins → MRsleep → Configuration** - every setting with an explanation, saving reloads MRsleep right away (permission `mradmin.config`). Without MRadmin everything is in `plugins/MRsleep/config.yml`, every option is commented there.

| Section | The most important settings | More |
|---|---|---|
| `general` | `language` (default `auto`), `fallback-language`, `server-language` | [Languages & messages](https://mrtechnology.marcelr.at/wiki/allgemein/sprachen/) |
| `sleep` | `percentage` (quota, default 50), `minimum-sleepers`, `disabled-worlds`, `clear-weather`; `ignore` (who is not counted) | [Sleep quota](schlafquote.md) |
| `sleep.display`, `sleep.chat` | Boss bar/action bar/title, colors, hide the vanilla display; sleep message, "Good morning!" | [Display & chat](anzeige.md) |
| `nosleep` | `cooldown` (15m), `max-nights` (3), `prevent-bed-enter`, `release-on-quit`, `broadcast` | [Sleep quota → /nosleep](schlafquote.md#nosleep-from-the-admin-side) |
| `team-limits` | How long support and staff may give a bypass or ban at most | [Team limits](admin-menue.md#how-long-the-team-may) |
| `discord` | Webhook URL, which events are sent, colors | [Discord & log](discord.md) |
| `updates` | on/off, source, check interval, notice on join | [Update check](#update-check) |
| `gui` | Refresh, quick-select durations, sounds | [Admin menu](admin-menue.md) |

- **Durations:** `d`, `h`, `m`, `s` can be combined (`15m`, `1h30m`, `2d`). A plain number counts as minutes.
- **Apply changes:** `/mrsleep reload` - no restart needed.
- **`config-version`** please do not change - MRsleep uses it to know whether the file has to be updated.
- **Broken umlauts** („Ã¤“ instead of „ä“)? Set the encoding in your editor to **UTF-8**.

## Language & messages
- `general.language: auto` (default): every player gets the language of their Minecraft client, otherwise `general.fallback-language` (default English).
- Discord logs have no single recipient - they use `general.server-language` (default `en`). On a German server, set `general.server-language: de`.
- All messages (chat, display, menus, Discord) are in `plugins/MRsleep/lang/<language>.yml` in MiniMessage format. An empty message (`""`) turns it off.
- Your own language, changing messages and more: [Languages & messages](https://mrtechnology.marcelr.at/wiki/allgemein/sprachen/).

## Update check
MRsleep checks on startup and then every few hours (`updates.interval-hours`, default 6) whether there is a new version:
- in the **console** with a download link,
- **in game** for players with `mrsleep.admin.update` as a clickable notice on join (`updates.notify-on-join`),
- any time with `/mrsleep version`.

Only versions for **your Minecraft version** are offered. Whoever uses a pre-release is also shown newer pre-releases (otherwise only with `updates.beta`).

## All settings
Every setting of `config.yml` with its default - the same fields as in MRadmin's browser form. The list is generated automatically from the plugin and therefore always matches this version.

<!-- auto:config -->
