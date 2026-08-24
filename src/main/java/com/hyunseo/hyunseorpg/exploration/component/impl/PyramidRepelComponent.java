package com.hyunseo.hyunseorpg.exploration.component.impl;

import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponent;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationComponentPhase;
import com.hyunseo.hyunseorpg.exploration.component.ExplorationEventContext;
import com.hyunseo.hyunseorpg.exploration.registry.ExplorationComponentSpec;
import com.hyunseo.hyunseorpg.exploration.pyramid.PyramidRoomService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Pushes the looter away from the pyramid before the delayed guardian spawn. */
public final class PyramidRepelComponent implements ExplorationComponent {
    private final PyramidRoomService rooms;

    public PyramidRepelComponent(PyramidRoomService rooms) { this.rooms = rooms; }

    @Override public String type() { return "pyramid_repel"; }
    @Override public ExplorationComponentPhase defaultPhase() { return ExplorationComponentPhase.PYRAMID_LOOT_TRIGGER; }

    @Override
    public void execute(ExplorationEventContext context, ExplorationComponentSpec spec) {
        Player player = context.runtime().looter() == null
                ? context.runtime().participants().stream().findFirst().map(org.bukkit.Bukkit::getPlayer).orElse(null)
                : org.bukkit.Bukkit.getPlayer(context.runtime().looter());
        if (player == null || !player.isOnline()) throw new IllegalStateException("pyramid looter is offline");
        Location room = rooms.room(context.runtime().structureId())
                .map(candidate -> new Location(player.getWorld(), candidate.origin().x() + 0.5D,
                        candidate.origin().y(), candidate.origin().z() + 0.5D))
                .orElseThrow(() -> new IllegalStateException("pyramid room is unavailable"));
        Vector away = player.getLocation().toVector().subtract(room.toVector());
        away.setY(0.0D);
        if (away.lengthSquared() < 0.01D) away = player.getLocation().getDirection().setY(0.0D);
        if (away.lengthSquared() < 0.01D) away = new Vector(0.0D, 0.0D, 1.0D);
        away.normalize().multiply(Math.max(0.35D, spec.decimal("horizontal-strength", 0.75D)));
        away.setY(Math.max(0.15D, spec.decimal("vertical-strength", 0.32D)));
        player.setVelocity(away);
        player.sendMessage(net.kyori.adventure.text.Component.text("피라미드의 지하 장치가 작동하며 무언가가 깨어납니다."));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_SAND_BREAK, 1.0F, 0.65F);
    }
}
