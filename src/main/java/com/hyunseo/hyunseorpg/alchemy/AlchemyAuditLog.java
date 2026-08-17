package com.hyunseo.hyunseorpg.alchemy;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** Small audit boundary for administrative and bypass decisions. */
public final class AlchemyAuditLog {
    private final JavaPlugin plugin;

    public AlchemyAuditLog(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void admin(String action, UUID actor, String details) {
        plugin.getLogger().info("[AlchemyAudit][ADMIN] action=" + action
                + " actor=" + (actor == null ? "console" : actor) + " details=" + details);
    }

    public void blocked(String path, String details) {
        plugin.getLogger().warning("[AlchemyAudit][BLOCKED] path=" + path + " details=" + details);
    }
}
