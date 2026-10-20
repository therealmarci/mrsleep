package at.marcelr.mrsleep.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Asks MRadmin whether a player is in admin mode ({@code at.marcelr.mradmin.api.MRadminApi.adminMode(UUID)}), found
 * at runtime - no dependency in either direction. Without MRadmin (or an older one without that API) nobody is in
 * admin mode. The method is looked up once per MRadmin instance, so a reload of MRadmin is picked up.
 */
public final class MRadminAdminMode {

    private static final String API_CLASS = "at.marcelr.mradmin.api.MRadminApi";

    private final Logger log;
    private Plugin resolvedFor;
    private MethodHandle adminMode;

    public MRadminAdminMode(Logger log) {
        this.log = log;
    }

    public boolean active(Player player) {
        Plugin mradmin = Bukkit.getPluginManager().getPlugin("MRadmin");
        if (mradmin == null || !mradmin.isEnabled()) {
            return false;
        }
        if (mradmin != resolvedFor) {
            resolvedFor = mradmin;
            adminMode = resolve(mradmin);
        }
        if (adminMode == null) {
            return false;
        }
        try {
            return (boolean) adminMode.invokeExact(player.getUniqueId());
        } catch (Throwable e) {
            log.warning("MRadmin admin mode check failed, ignoring it from now on: " + e);
            adminMode = null;
            return false;
        }
    }

    private MethodHandle resolve(Plugin mradmin) {
        try {
            Class<?> api = Class.forName(API_CLASS, true, mradmin.getClass().getClassLoader());
            return MethodHandles.publicLookup().findStatic(api, "adminMode",
                    MethodType.methodType(boolean.class, UUID.class));
        } catch (ReflectiveOperationException e) {
            // Older MRadmin without this API.
            return null;
        }
    }
}
