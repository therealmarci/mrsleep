package at.marcelr.mrsleep.api;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Asks MRpvp ({@code at.marcelr.mrpvp.api.MRpvpApi}) whether a player is in a duel (or MRpvp's arena editor) and
 * which world is its arena world - found at runtime, no dependency in either direction. Without MRpvp nobody is in
 * a duel and there is no arena world. The methods are looked up once per MRpvp instance, so a reload is picked up.
 */
public final class MRpvpGame {

    private static final String API_CLASS = "at.marcelr.mrpvp.api.MRpvpApi";

    private final Logger log;
    private Plugin resolvedFor;
    private MethodHandle inGame;
    private MethodHandle isArenaWorld;

    public MRpvpGame(Logger log) {
        this.log = log;
    }

    public boolean inGame(Player player) {
        if (!resolve()) {
            return false;
        }
        try {
            return inGame != null && (boolean) inGame.invokeExact(player.getUniqueId());
        } catch (Throwable e) {
            fail(e);
            return false;
        }
    }

    public boolean isArenaWorld(World world) {
        if (!resolve()) {
            return false;
        }
        try {
            return isArenaWorld != null && (boolean) isArenaWorld.invokeExact(world.getName());
        } catch (Throwable e) {
            fail(e);
            return false;
        }
    }

    private boolean resolve() {
        Plugin mrpvp = Bukkit.getPluginManager().getPlugin("MRpvp");
        if (mrpvp == null || !mrpvp.isEnabled()) {
            return false;
        }
        if (mrpvp != resolvedFor) {
            resolvedFor = mrpvp;
            try {
                Class<?> api = Class.forName(API_CLASS, true, mrpvp.getClass().getClassLoader());
                inGame = MethodHandles.publicLookup().findStatic(api, "inGame",
                        MethodType.methodType(boolean.class, UUID.class));
                isArenaWorld = MethodHandles.publicLookup().findStatic(api, "isArenaWorld",
                        MethodType.methodType(boolean.class, String.class));
            } catch (ReflectiveOperationException e) {
                inGame = null;
                isArenaWorld = null;
            }
        }
        return true;
    }

    private void fail(Throwable e) {
        log.warning("MRpvp check failed, ignoring MRpvp from now on: " + e);
        inGame = null;
        isArenaWorld = null;
    }
}
