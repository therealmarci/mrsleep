package at.marcelr.mrsleep.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

/**
 * Players that are no real players: MRchunks' dummy players (they sit on chunk loaders) and NPCs of other plugins
 * (Citizens and others mark them with the metadata "NPC"). MRsleep ignores them completely - they never count towards
 * the quota and do not show up in lists.
 */
public final class Npcs {

    /** MRchunks marks its dummies with this key in the player's data container. */
    private static final NamespacedKey MRCHUNKS_DUMMY = new NamespacedKey("mrchunks", "dummy");

    private Npcs() {
    }

    public static boolean is(Player player) {
        return player.hasMetadata("NPC") || player.getPersistentDataContainer().has(MRCHUNKS_DUMMY);
    }
}
