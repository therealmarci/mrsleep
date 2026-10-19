package at.marcelr.mrsleep;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Tracks /nosleep blocks per world (spanning one or more nights), cooldowns and admin cooldown bypasses. */
public final class NoSleepManager {

    public static final String PERM_NO_COOLDOWN = "mrsleep.nosleep.nocooldown";

    /** From this time of day on, beds work in clear weather. */
    public static final long NIGHT_START = 12_542L;
    /** From this time of day on, beds no longer work and the night counts as over. */
    public static final long NIGHT_END = 23_460L;

    private final MRSleep plugin;
    private final File dataFile;
    /** world -> (player -> block), insertion ordered so names show up in the order they blocked. */
    private final Map<UUID, Map<UUID, Block>> blocks = new HashMap<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<UUID, Long> bypassUntil = new HashMap<>();
    private final Map<UUID, Ban> bans = new HashMap<>();

    /** Exclusion from /nosleep. {@code until == Long.MAX_VALUE} means permanent. */
    public record Ban(long until, String reason, String by) {

        public boolean permanent() {
            return until == Long.MAX_VALUE;
        }

        public long remaining() {
            return permanent() ? Long.MAX_VALUE : until - System.currentTimeMillis();
        }
    }

    /** Blocks all nights with a key in [startKey, endKey]. */
    /**
     * @param charged whether the /nosleep cooldown was already started for this block. A block set during the day
     *                is "pending" until sleeping becomes possible; lifting it before that costs no cooldown.
     */
    private record Block(long startKey, long endKey, boolean charged) {

        Block charge() {
            return new Block(startKey, endKey, true);
        }

        boolean expired(long currentKey) {
            return currentKey > endKey;
        }

        int remaining(long currentKey) {
            long total = endKey - startKey + 1;
            return (int) Math.max(0, Math.min(total, endKey - currentKey + 1));
        }
    }

    public NoSleepManager(MRSleep plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    /**
     * Identifies the night a /nosleep applies to: during the day it is the upcoming night,
     * at night it is the current one. Both share the same in-game day number.
     */
    public static long nightKey(World world) {
        long day = world.getFullTime() / 24_000L;
        return world.getTime() >= NIGHT_END ? day + 1 : day;
    }

    // ---------------------------------------------------------------- blocks

    /** @return blocking players with the number of nights each still blocks (including the current/upcoming one) */
    public Map<UUID, Integer> activeBlockers(World world) {
        Map<UUID, Block> worldBlocks = blocks.get(world.getUID());
        if (worldBlocks == null || worldBlocks.isEmpty()) {
            return Map.of();
        }
        long key = nightKey(world);
        Map<UUID, Integer> result = new LinkedHashMap<>();
        worldBlocks.forEach((player, block) -> {
            if (!block.expired(key)) {
                result.put(player, block.remaining(key));
            }
        });
        return result;
    }

    public boolean isBlocked(World world) {
        return !activeBlockers(world).isEmpty();
    }

    /** @return nights the player still blocks in this world, 0 if none */
    public int remainingNights(World world, UUID player) {
        Integer nights = activeBlockers(world).get(player);
        return nights == null ? 0 : nights;
    }

    /** @return number of nights until sleeping is possible again (the longest active block) */
    public static int longest(Map<UUID, Integer> blockers) {
        int max = 0;
        for (int nights : blockers.values()) {
            max = Math.max(max, nights);
        }
        return max;
    }

    /** Blocks the current/upcoming night and the following {@code nights - 1} nights, replacing an existing block. */
    /**
     * Blocks the current/upcoming night and the following {@code nights - 1} nights, replacing an existing block.
     * If sleeping is already possible the cooldown starts right away, otherwise when the night begins.
     */
    public void setBlock(World world, UUID player, int nights) {
        long key = nightKey(world);
        boolean chargeNow = SleepManager.sleepPossible(world);
        blocks.computeIfAbsent(world.getUID(), k -> new LinkedHashMap<>())
                .put(player, new Block(key, key + Math.max(1, nights) - 1, chargeNow));
        if (chargeNow) {
            markUsed(player);
        } else {
            save();
        }
    }

    /** @return true if the player's block in this world waits for the night, i.e. its cooldown has not started */
    public boolean isPending(World world, UUID player) {
        Map<UUID, Block> worldBlocks = blocks.get(world.getUID());
        Block block = worldBlocks == null ? null : worldBlocks.get(player);
        return block != null && !block.expired(nightKey(world)) && !block.charged();
    }

    /** @return true if the player has a pending block in any world */
    public boolean isPendingAnywhere(UUID player) {
        for (World world : Bukkit.getWorlds()) {
            if (isPending(world, player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Starts the cooldown of all pending blocks once sleeping is possible in the world.
     * @return players whose cooldown started now
     */
    public List<UUID> chargePending(World world) {
        Map<UUID, Block> worldBlocks = blocks.get(world.getUID());
        if (worldBlocks == null || worldBlocks.isEmpty() || !SleepManager.sleepPossible(world)) {
            return List.of();
        }
        long key = nightKey(world);
        List<UUID> charged = new ArrayList<>();
        for (Map.Entry<UUID, Block> entry : worldBlocks.entrySet()) {
            Block block = entry.getValue();
            if (!block.charged() && !block.expired(key) && key >= block.startKey()) {
                entry.setValue(block.charge());
                lastUse.put(entry.getKey(), System.currentTimeMillis());
                charged.add(entry.getKey());
            }
        }
        if (!charged.isEmpty()) {
            save();
        }
        return charged;
    }

    public boolean removeBlock(World world, UUID player) {
        Map<UUID, Block> worldBlocks = blocks.get(world.getUID());
        boolean removed = worldBlocks != null && worldBlocks.remove(player) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean clear(World world) {
        boolean wasBlocked = isBlocked(world);
        blocks.remove(world.getUID());
        save();
        return wasBlocked;
    }

    /** Removes the player from all blocks. @return worlds in which a block of the player was active */
    public List<World> release(UUID player) {
        List<World> affected = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (remainingNights(world, player) > 0) {
                affected.add(world);
            }
            Map<UUID, Block> worldBlocks = blocks.get(world.getUID());
            if (worldBlocks != null) {
                worldBlocks.remove(player);
            }
        }
        if (!affected.isEmpty()) {
            save();
        }
        return affected;
    }

    /** Removes blocks whose last night is over. @return players whose block expired */
    public List<UUID> purgeExpired(World world) {
        Map<UUID, Block> worldBlocks = blocks.get(world.getUID());
        if (worldBlocks == null || worldBlocks.isEmpty()) {
            return List.of();
        }
        long key = nightKey(world);
        List<UUID> expired = new ArrayList<>();
        Iterator<Map.Entry<UUID, Block>> it = worldBlocks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Block> entry = it.next();
            if (entry.getValue().expired(key)) {
                expired.add(entry.getKey());
                it.remove();
            }
        }
        if (!expired.isEmpty()) {
            save();
        }
        return expired;
    }

    public static String name(UUID player) {
        String name = Bukkit.getOfflinePlayer(player).getName();
        return name == null ? player.toString().substring(0, 8) : name;
    }

    public static String names(Collection<UUID> players) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : players) {
            names.add(name(uuid));
        }
        return String.join(", ", names);
    }

    // ------------------------------------------------------------- cooldowns

    /** @return remaining cooldown in ms, 0 if /nosleep can be used now */
    public long remainingCooldown(Player player) {
        if (player.hasPermission(PERM_NO_COOLDOWN) || bypassRemaining(player.getUniqueId()) > 0) {
            return 0;
        }
        Long last = lastUse.get(player.getUniqueId());
        if (last == null) {
            return 0;
        }
        return Math.max(0, last + plugin.settings().cooldownMillis() - System.currentTimeMillis());
    }

    /** Like {@link #remainingCooldown(Player)}, also for offline players (then without the permission check). */
    public long remainingCooldown(UUID player) {
        Player online = Bukkit.getPlayer(player);
        if (online != null) {
            return remainingCooldown(online);
        }
        Long last = lastUse.get(player);
        if (last == null || bypassRemaining(player) > 0) {
            return 0;
        }
        return Math.max(0, last + plugin.settings().cooldownMillis() - System.currentTimeMillis());
    }

    /** Players whose /nosleep cooldown is still running (offline players included). */
    public java.util.Set<UUID> onCooldown() {
        java.util.Set<UUID> result = new java.util.HashSet<>();
        for (UUID player : lastUse.keySet()) {
            if (remainingCooldown(player) > 0) {
                result.add(player);
            }
        }
        return result;
    }

    public void markUsed(UUID player) {
        lastUse.put(player, System.currentTimeMillis());
        save();
    }

    public void resetCooldown(UUID player) {
        lastUse.remove(player);
        save();
    }

    // --------------------------------------------------------------- bypass

    public void setBypass(UUID player, long durationMillis) {
        bypassUntil.put(player, System.currentTimeMillis() + durationMillis);
        save();
    }

    public boolean removeBypass(UUID player) {
        boolean had = bypassRemaining(player) > 0;
        bypassUntil.remove(player);
        save();
        return had;
    }

    public long bypassRemaining(UUID player) {
        Long until = bypassUntil.get(player);
        if (until == null) {
            return 0;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) {
            bypassUntil.remove(player);
            return 0;
        }
        return remaining;
    }

    /** @return active bypasses (player -> remaining ms) */
    public Map<UUID, Long> activeBypasses() {
        Map<UUID, Long> result = new HashMap<>();
        for (UUID uuid : List.copyOf(bypassUntil.keySet())) {
            long remaining = bypassRemaining(uuid);
            if (remaining > 0) {
                result.put(uuid, remaining);
            }
        }
        return result;
    }

    // ----------------------------------------------------------------- bans

    /** @param durationMillis duration, or {@link Settings#PERMANENT} */
    public void ban(UUID player, long durationMillis, String reason, String by) {
        long until = durationMillis == Settings.PERMANENT ? Long.MAX_VALUE : System.currentTimeMillis() + durationMillis;
        bans.put(player, new Ban(until, reason, by));
        save();
    }

    public boolean unban(UUID player) {
        boolean had = ban(player) != null;
        bans.remove(player);
        save();
        return had;
    }

    /** @return the active ban, or null */
    public Ban ban(UUID player) {
        Ban ban = bans.get(player);
        if (ban != null && ban.remaining() <= 0) {
            bans.remove(player);
            return null;
        }
        return ban;
    }

    public Map<UUID, Ban> activeBans() {
        Map<UUID, Ban> result = new LinkedHashMap<>();
        for (UUID uuid : List.copyOf(bans.keySet())) {
            Ban ban = ban(uuid);
            if (ban != null) {
                result.put(uuid, ban);
            }
        }
        return result;
    }

    // ---------------------------------------------------------- persistence

    public void load() {
        blocks.clear();
        lastUse.clear();
        bypassUntil.clear();
        bans.clear();
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        readTimes(data.getConfigurationSection("cooldowns"), lastUse);
        readTimes(data.getConfigurationSection("bypass"), bypassUntil);

        ConfigurationSection banSection = data.getConfigurationSection("bans");
        if (banSection != null) {
            for (String key : banSection.getKeys(false)) {
                UUID uuid = parseUuid(key);
                if (uuid != null) {
                    bans.put(uuid, new Ban(
                            banSection.getLong(key + ".until"),
                            banSection.getString(key + ".reason", ""),
                            banSection.getString(key + ".by", "?")));
                }
            }
        }

        ConfigurationSection worlds = data.getConfigurationSection("blocks");
        if (worlds == null) {
            return;
        }
        for (String worldKey : worlds.getKeys(false)) {
            ConfigurationSection players = worlds.getConfigurationSection(worldKey);
            UUID worldId = parseUuid(worldKey);
            if (players == null || worldId == null) {
                continue;
            }
            Map<UUID, Block> worldBlocks = new LinkedHashMap<>();
            for (String playerKey : players.getKeys(false)) {
                UUID playerId = parseUuid(playerKey);
                if (playerId != null) {
                    worldBlocks.put(playerId, new Block(
                            players.getLong(playerKey + ".start"),
                            players.getLong(playerKey + ".end"),
                            // blocks saved before 0.9.4 had their cooldown charged on creation
                            players.getBoolean(playerKey + ".charged", true)));
                }
            }
            blocks.put(worldId, worldBlocks);
        }
    }

    private static void readTimes(ConfigurationSection section, Map<UUID, Long> target) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            UUID uuid = parseUuid(key);
            if (uuid != null) {
                target.put(uuid, section.getLong(key));
            }
        }
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void save() {
        long now = System.currentTimeMillis();
        long cooldown = plugin.settings().cooldownMillis();
        lastUse.values().removeIf(last -> last + cooldown < now);
        bypassUntil.values().removeIf(until -> until < now);
        bans.values().removeIf(ban -> ban.remaining() <= 0);

        YamlConfiguration data = new YamlConfiguration();
        bans.forEach((uuid, ban) -> {
            data.set("bans." + uuid + ".until", ban.until());
            data.set("bans." + uuid + ".reason", ban.reason());
            data.set("bans." + uuid + ".by", ban.by());
        });
        lastUse.forEach((uuid, time) -> data.set("cooldowns." + uuid, time));
        bypassUntil.forEach((uuid, time) -> data.set("bypass." + uuid, time));
        blocks.forEach((world, worldBlocks) -> worldBlocks.forEach((player, block) -> {
            data.set("blocks." + world + "." + player + ".start", block.startKey());
            data.set("blocks." + world + "." + player + ".end", block.endKey());
            data.set("blocks." + world + "." + player + ".charged", block.charged());
        }));
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save data.yml", e);
        }
    }
}
