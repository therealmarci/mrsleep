# Discord & log

## Set up a Discord webhook
1. In Discord: **Edit channel → Integrations → Webhooks → New webhook** and **copy the webhook URL**.
2. In the config set `discord.enabled: true` and enter the URL at `discord.webhook-url`.
3. `/mrsleep reload`, then check with **`/mrsleep discord test`** - the command tells you whether the message arrived or why not.

Under `discord.events` you set per event whether it is sent and in which color. The texts are in the language file under `discord` (in `general.server-language`). `@everyone` never pings anyone.

| Event | When |
|---|---|
| `nosleep-activated` · `nosleep-deactivated` | A player blocks or lifts a block |
| `nosleep-quit` · `nosleep-expired` | A block ends by leaving or expires |
| `night-start-blocked` · `bed-blocked` | A night starts blocked · someone wants to sleep in a blocked night |
| `night-skipped` | Night skipped |
| `admin-clear` · `admin-release` | The team removes blocks |
| `admin-bypass` · `admin-bypass-remove` · `admin-cooldown-reset` | Cooldown lifted or reset |
| `admin-ban` · `admin-unban` | Ban from `/nosleep` |
| `admin-reload` | Config reloaded |

More settings: `username`, `avatar-url`, `use-embeds`, `footer`.

## Without an own webhook
> [!TIP]
> With MRadmin it also works through its Discord log: `discord.enabled: true`, leave `webhook-url` empty and set `discord.use-mradmin-webhook: true`.

## Log in MRadmin
With MRadmin every event is also in the **log** of the web interface (up to 14 days) - even when Discord is off.
