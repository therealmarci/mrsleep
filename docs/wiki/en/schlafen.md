# Sleeping & sleep quota

On a server with MRsleep not everyone has to go to bed for the night to pass - a part of the players is enough (the **sleep quota**, usually 50 %).

## How a night works
1. Go to bed. At the top of the screen you see the progress, e.g. **"1/2 sleeping"**.
2. The chat shows "Alex went to bed (1/2)" so the others know.
3. As soon as enough players have been in bed for about **5 seconds**, it becomes morning - "Good morning!".

Every world votes on its own: whoever lies in a bed in the farm world only skips the night there. Nobody sleeps in the Nether and the End.

## Who is counted?
Only players who *could* sleep. Not counted are for example:
- spectators,
- players marked as AFK,
- bots and camera accounts,
- team members in admin mode and players fighting a duel.

If you are not counted yourself, MRsleep tells you why when you go to bed. You can see your status any time with `/nosleep info`.

## The night is not skipped?
- **Someone blocks the night with `/nosleep`** - the progress turns red and shows the name. Maybe they are building a mob farm. More: [/nosleep](nosleep.md).
- **Too few are sleeping** - the progress shows how many are missing.
