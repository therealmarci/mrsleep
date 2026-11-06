# /nosleep - keep the night

You need the night - for a mob farm, for building in the dark or for a screenshot? With `/nosleep` you prevent it from being skipped.

| Input | Effect |
|---|---|
| `/nosleep` | Blocks the current night (during the day: the next one). If you already have a block, it is **lifted**. |
| `/nosleep 3` | Blocks the next 3 nights (the server sets the maximum - default 3). |
| `/nosleep off` | Lifts your own block (also `aus` or `cancel`). |
| `/nosleep info` | Shows your status: who is sleeping, whether you are counted, who is blocking, your cooldown. |

## Good to know
- As long as **any** player blocks, the night is not skipped. Everyone else sees who is blocking.
- Depending on the server you can still go to bed in a blocked night (e.g. to set your spawn point) - it just is not skipped.
- If you go offline, your block usually ends.
- Blocks apply **per world**.

## Cooldown
After a block you have to wait a while before you can block again (default 15 minutes). The cooldown only starts **when the blocked night begins**: whoever blocks during the day and lifts it before nightfall gets none. `/nosleep info` shows how long you still have to wait.

> [!NOTE]
> The team can lift your cooldown or exclude you from `/nosleep` if the blocks get out of hand.
