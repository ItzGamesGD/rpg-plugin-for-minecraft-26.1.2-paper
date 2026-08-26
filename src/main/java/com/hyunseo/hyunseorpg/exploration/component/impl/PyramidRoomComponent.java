package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/** Preflights the bounded Pyramid underground room after canonical loot extraction. */
public final class PyramidRoomComponent implements ExplorationComponent {
    private final com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService rooms;

    public PyramidRoomComponent(com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService rooms) {
        this.rooms = rooms;
    }

    @Override public String type() { return "pyramid_room"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.PYRAMID_LOOT_TRIGGER; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) throws Exception {
        if (!"desert_pyramid".equals(context.record().structureType())) {
            throw new IllegalArgumentException("pyramid_room requires desert_pyramid");
        }
        rooms.prepare(context, spec);
        context.world().ifPresent(world -> {
            Location center = new Location(world, context.record().bounds().centerX(),
                    context.record().bounds().minY(), context.record().bounds().centerZ());
            world.spawnParticle(Particle.FALLING_DUST, center, 18, 2.5D, 0.2D, 2.5D, 0.01D);
            world.playSound(center, Sound.BLOCK_SANDSTONE_BREAK, 0.45F, 0.8F);
            scheduleTelegraph(context, center, 40L, 12, 0.35F);
            scheduleTelegraph(context, center, 80L, 18, 0.45F);
            scheduleTelegraph(context, center, 110L, 26, 0.60F);
            scheduleTelegraph(context, center, 130L, 34, 0.75F);
        });
        String actionId = spec.string("action-id", "pyramid_room_reveal");
        long delay = Math.max(0L, spec.integer("reveal-delay-ticks", 140));
        String nextPhase = spec.string("reveal-phase", "pyramid_room_reveal");
        if (!context.sequenceScheduler().schedule(context, actionId, delay, nextPhase)) {
            throw new IllegalStateException("pyramid room reveal already scheduled: " + actionId);
        }
    }
    private void scheduleTelegraph(ExplorationEventContext context, Location center, long delay,
                                   int particles, float volume) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            if (context.runtime().tracker().isClosed()) return;
            context.world().ifPresent(world -> {
                world.spawnParticle(Particle.FALLING_DUST, center, particles,
                        1.5D + particles * 0.04D, 0.15D, 1.5D + particles * 0.04D, 0.01D);
                world.playSound(center, Sound.BLOCK_SANDSTONE_BREAK, volume,
                        delay >= 110L ? 0.65F : 0.8F);
            });
        }, delay);
        context.runtime().tracker().track(task::cancel);
    }
}
