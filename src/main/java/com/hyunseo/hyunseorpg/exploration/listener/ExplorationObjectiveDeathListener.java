package com.hyunseo.hyunseorpg.exploration.listener;

import com.hyunseo.hyunseorpg.exploration.runtime.ExplorationRuntimeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/** Converts only explicit Bukkit death events into exploration objective confirmations. */
public final class ExplorationObjectiveDeathListener implements Listener {
    private final ExplorationRuntimeManager runtimes;

    public ExplorationObjectiveDeathListener(ExplorationRuntimeManager runtimes) {
        this.runtimes = runtimes;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        runtimes.confirmObjectiveDeath(event.getEntity().getUniqueId());
    }
}
