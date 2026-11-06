# FAQ

### I am in bed, but the night is not skipped.
Run `/mrsleep debug` or open the [admin menu](admin-menue.md): it shows for every player whether they are counted (and why not), whether they sleep and whether someone blocks. Most common cause: through `*` a player also has `mrsleep.exempt` - see [wildcards](permissions.md#wildcards).

### The game rule `playersSleepingPercentage` is set to 100.
That is intended, the quota comes from `sleep.percentage` - [why](schlafquote.md#worlds).

### What counts as "one night"?
Whoever uses `/nosleep` during the day blocks the next night, whoever uses it at night blocks the current one. Blocks apply per world.

### Do my admins have a cooldown or not?
The cooldown also applies to OPs. No cooldown only with `mrsleep.nosleep.nocooldown` (in no group) or a bypass.

### I found a bug or have a wish.
Gladly via the **[form on the website](https://mrtechnology.marcelr.at/report/mrsleep/)** - ideally with the output of `/mrsleep version` and `/mrsleep debug`.
