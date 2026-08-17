package com.hyunseo.hyunseorpg.quest;

import com.hyunseo.hyunseorpg.core.config.ConfigService;
import com.hyunseo.hyunseorpg.core.event.RPGMobKillEvent;
import com.hyunseo.hyunseorpg.economy.CoinService;
import com.hyunseo.hyunseorpg.exp.ExpService;
import com.hyunseo.hyunseorpg.item.RPGItemService;
import com.hyunseo.hyunseorpg.player.PlayerDataService;
import com.hyunseo.hyunseorpg.player.PlayerRPGData;
import com.hyunseo.hyunseorpg.quest.availability.ContentAvailabilityService;
import com.hyunseo.hyunseorpg.quest.availability.ItemObtainabilityContext;
import com.hyunseo.hyunseorpg.quest.availability.ItemObtainabilityResult;
import com.hyunseo.hyunseorpg.quest.availability.MonsterEligibilityContext;
import com.hyunseo.hyunseorpg.quest.availability.MonsterEligibilityResult;
import com.hyunseo.hyunseorpg.quest.availability.QuestTargetCandidate;
import com.hyunseo.hyunseorpg.quest.availability.QuestTargetSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Stores generated quests; all candidate selection is delegated to ContentAvailabilityService. */
public final class AutoQuestService {
    private final ConfigService config;
    private final PlayerDataService playerData;
    private final RPGItemService itemService;
    private final CoinService coins;
    private final ExpService exp;
    private final ContentAvailabilityService availability;
    private final Set<UUID> processing = new HashSet<>();

    public AutoQuestService(ConfigService config, PlayerDataService playerData, RPGItemService itemService,
                            CoinService coins, ExpService exp, ContentAvailabilityService availability) {
        this.config = config;
        this.playerData = playerData;
        this.itemService = itemService;
        this.coins = coins;
        this.exp = exp;
        this.availability = availability;
    }

    /** Compatibility entry point. It intentionally does not auto-accept quests. */
    public void ensureQuests(Player player) {
        refresh(player);
    }

    public void refresh(Player player) {
        PlayerRPGData data = playerData.getOrLoad(player);
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (AutoQuestData quest : new ArrayList<>(data.getAutoQuests().values())) {
            if (quest.isExpired(now)) {
                data.removeAutoQuest(quest.slot());
                data.incrementFailedAutoQuestCount();
                setCooldown(data, "after-fail-seconds");
                changed = true;
                continue;
            }
            if (!stillDefined(quest)) {
                quest.setStatus(AutoQuestStatus.CONFIG_INVALIDATED);
                data.removeAutoQuest(quest.slot());
                changed = true;
                player.sendMessage(Component.text("퀘스트 대상 설정이 변경되어 안전하게 취소되었습니다: "
                        + quest.displayName(), NamedTextColor.YELLOW));
            }
        }
        if (changed) playerData.savePlayer(player);
    }

    public List<AutoQuestData> getQuests(Player player) {
        refresh(player);
        return playerData.getOrLoad(player).getAutoQuests().values().stream()
                .sorted(Comparator.comparingInt(AutoQuestData::slot)).toList();
    }

    public boolean accept(Player player) {
        return accept(player, null);
    }

    public boolean accept(Player player, AutoQuestType forcedType) {
        if (!processing.add(player.getUniqueId())) return false;
        try {
            refresh(player);
            PlayerRPGData data = playerData.getOrLoad(player);
            if (remainingCooldownSeconds(player) > 0L || data.getAutoQuests().size() >= maxActive()) return false;
            int slot = firstFreeSlot(data);
            if (slot < 0) return false;
            AutoQuestData generated = generate(player, slot, forcedType);
            if (generated == null) return false;
            data.setAutoQuest(generated);
            setCooldown(data, "after-accept-seconds");
            playerData.savePlayer(player);
            player.sendMessage(Component.text("새 퀘스트를 수락했습니다: " + generated.displayName(), NamedTextColor.GREEN));
            return true;
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    public int displayedProgress(Player player, AutoQuestData quest) {
        AutoQuestObjectiveData objective = quest.objectives().getFirst();
        if (quest.type() != AutoQuestType.ITEM_DELIVERY) return objective.progress();
        return Math.min(objective.amount(), countMatchingItems(player, objective));
    }

    public boolean isComplete(Player player, AutoQuestData quest) {
        for (AutoQuestObjectiveData objective : quest.objectives()) {
            int progress = quest.type() == AutoQuestType.ITEM_DELIVERY
                    ? countMatchingItems(player, objective) : objective.progress();
            if (progress < objective.amount()) return false;
        }
        return true;
    }

    public List<String> progressLines(Player player, AutoQuestData quest) {
        List<String> lines = new ArrayList<>();
        for (AutoQuestObjectiveData objective : quest.objectives()) {
            int progress = quest.type() == AutoQuestType.ITEM_DELIVERY
                    ? Math.min(objective.amount(), countMatchingItems(player, objective)) : objective.progress();
            lines.add("목표: " + objective.displayName() + " " + progress + "/" + objective.amount());
        }
        return List.copyOf(lines);
    }

    public boolean complete(Player player, int slot) {
        if (!processing.add(player.getUniqueId())) return false;
        try {
            PlayerRPGData data = playerData.getOrLoad(player);
            AutoQuestData quest = data.getAutoQuest(slot);
            if (quest == null || quest.isExpired(System.currentTimeMillis())) {
                fail(data, slot, "after-fail-seconds");
                playerData.savePlayer(player);
                return false;
            }
            if (!isComplete(player, quest)) return false;
            List<Removal> plan = quest.type() == AutoQuestType.ITEM_DELIVERY
                    ? buildRemovalPlan(player, quest) : List.of();
            if (quest.type() == AutoQuestType.ITEM_DELIVERY && !planFulfillsObjectives(plan, quest)) return false;
            if (!planStillValid(player, quest, plan)) return false;
            applyRemovalPlan(player, plan);
            if (quest.rewardCoins() > 0L) coins.addCoins(player, quest.rewardCoins());
            if (quest.rewardExp() > 0L) exp.giveBaseExp(player, quest.rewardExp());
            quest.setStatus(AutoQuestStatus.COMPLETED);
            data.removeAutoQuest(slot);
            data.incrementCompletedAutoQuestCount();
            setCooldown(data, "after-complete-seconds");
            playerData.savePlayer(player);
            player.sendMessage(Component.text("퀘스트 완료: " + quest.displayName(), NamedTextColor.GOLD));
            return true;
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    public boolean abandon(Player player, int slot) {
        if (!processing.add(player.getUniqueId())) return false;
        try {
            PlayerRPGData data = playerData.getOrLoad(player);
            AutoQuestData quest = data.getAutoQuest(slot);
            if (quest == null) return false;
            quest.setStatus(AutoQuestStatus.ABANDONED);
            data.removeAutoQuest(slot);
            data.incrementAbandonedAutoQuestCount();
            setCooldown(data, "after-abandon-seconds");
            playerData.savePlayer(player);
            player.sendMessage(Component.text("퀘스트를 포기했습니다: " + quest.displayName(), NamedTextColor.YELLOW));
            return true;
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    public void clear(Player player) {
        if (!processing.add(player.getUniqueId())) return;
        try {
            PlayerRPGData data = playerData.getOrLoad(player);
            new ArrayList<>(data.getAutoQuests().keySet()).forEach(data::removeAutoQuest);
            playerData.savePlayer(player);
        } finally {
            processing.remove(player.getUniqueId());
        }
    }

    public void resetCooldown(Player player) {
        PlayerRPGData data = playerData.getOrLoad(player);
        data.setAutoQuestCooldownUntil(0L);
        playerData.savePlayer(player);
    }

    public void onMobKill(RPGMobKillEvent event) {
        Player player = event.getKiller();
        if (player == null) return;
        PlayerRPGData data = playerData.getOrLoad(player);
        boolean changed = false;
        for (AutoQuestData quest : data.getAutoQuests().values()) {
            if (quest.type() != AutoQuestType.HUNT || quest.isComplete() || quest.isExpired(System.currentTimeMillis())) continue;
            boolean matched = false;
            for (AutoQuestObjectiveData objective : quest.objectives()) {
                if (!matches(event, objective)) continue;
                quest.addProgress(objective.source(), objective.target(), 1);
                matched = true;
            }
            if (!matched) continue;
            changed = true;
            if (quest.isComplete()) {
                player.sendMessage(Component.text("퀘스트 목표를 달성했습니다. 퀘스트 메뉴에서 완료하세요.", NamedTextColor.AQUA));
            }
        }
        if (changed) playerData.savePlayer(player);
    }

    public long remainingCooldownSeconds(Player player) {
        long remaining = playerData.getOrLoad(player).getAutoQuestCooldownUntil() - System.currentTimeMillis();
        return Math.max(0L, (remaining + 999L) / 1000L);
    }

    public int maxActive() {
        return Math.max(1, config.getQuestsInt("auto.max-active", 2));
    }

    private AutoQuestData generate(Player player, int slot, AutoQuestType forcedType) {
        int attempts = Math.max(1, config.getQuestsInt("auto.generation.max-attempts", 30));
        for (int attempt = 0; attempt < attempts; attempt++) {
            AutoQuestType type = forcedType == null ? selectType() : forcedType;
            List<QuestTargetCandidate> candidates = type == AutoQuestType.HUNT
                    ? availability.eligibleHuntTargets(player) : availability.eligibleItemTargets(player);
            if (candidates.isEmpty()) {
                if (forcedType != null) return null;
                type = type == AutoQuestType.HUNT ? AutoQuestType.ITEM_DELIVERY : AutoQuestType.HUNT;
                candidates = type == AutoQuestType.HUNT
                        ? availability.eligibleHuntTargets(player) : availability.eligibleItemTargets(player);
                if (candidates.isEmpty()) continue;
            }
            List<QuestTargetCandidate> selected = selectTargets(type, candidates);
            if (selected.isEmpty()) continue;
            return create(slot, type, selected);
        }
        return null;
    }

    private AutoQuestType selectType() {
        int huntWeight = Math.max(0, config.getQuestsInt("auto.types.hunt.weight", 70));
        int itemWeight = Math.max(0, config.getQuestsInt("auto.types.item-delivery.weight", 30));
        if (huntWeight == 0 && itemWeight == 0) return AutoQuestType.HUNT;
        return ThreadLocalRandom.current().nextInt(huntWeight + itemWeight) < huntWeight
                ? AutoQuestType.HUNT : AutoQuestType.ITEM_DELIVERY;
    }

    private int selectRequiredAmount(AutoQuestType type) {
        String root = type == AutoQuestType.HUNT ? "auto.types.hunt.amount-per-target" : "auto.types.item-delivery.amount";
        int defaultMin = type == AutoQuestType.HUNT ? 5 : 8;
        int defaultMax = type == AutoQuestType.HUNT ? 30 : 64;
        int min = Math.max(1, config.getQuestsInt(root + ".min", defaultMin));
        int max = Math.max(min, config.getQuestsInt(root + ".max", defaultMax));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private List<QuestTargetCandidate> selectTargets(AutoQuestType type, List<QuestTargetCandidate> candidates) {
        int requested = type == AutoQuestType.HUNT ? selectTargetTypeCount() : 1;
        List<QuestTargetCandidate> pool = new ArrayList<>(candidates);
        java.util.Collections.shuffle(pool, ThreadLocalRandom.current());
        return pool.stream().limit(Math.min(requested, pool.size())).toList();
    }

    private int selectTargetTypeCount() {
        int min = Math.max(1, config.getQuestsInt("auto.types.hunt.target-count.min", 1));
        int max = Math.max(min, config.getQuestsInt("auto.types.hunt.target-count.max", 1));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private AutoQuestData create(int slot, AutoQuestType type, List<QuestTargetCandidate> candidates) {
        long now = System.currentTimeMillis();
        long limit = Math.max(1L, config.getQuestsLong("auto.time-limit-seconds", 1800L));
        long expires = now + limit * 1000L;
        long baseCoins = Math.max(0L, config.getQuestsLong("auto.rewards.coin.base", 25L));
        long coinsPerAmount = Math.max(0L, config.getQuestsLong("auto.rewards.coin.per-required", 2L));
        long baseExp = Math.max(0L, config.getQuestsLong("auto.rewards.rpg-exp.base", 20L));
        long expPerAmount = Math.max(0L, config.getQuestsLong("auto.rewards.rpg-exp.per-required", 1L));
        List<AutoQuestObjectiveData> objectives = candidates.stream().map(candidate -> new AutoQuestObjectiveData(
                candidate.source(), candidate.id(), candidate.displayName(), selectRequiredAmount(type), 0, candidate.difficulty())).toList();
        long totalAmount = objectives.stream().mapToLong(AutoQuestObjectiveData::amount).sum();
        long difficulty = objectives.stream().mapToLong(AutoQuestObjectiveData::difficulty).sum();
        long rewardCoins = baseCoins + coinsPerAmount * totalAmount + Math.max(0L, difficulty - objectives.size()) * 2L;
        long rewardExp = baseExp + expPerAmount * totalAmount + Math.max(0L, difficulty - objectives.size());
        String name = type == AutoQuestType.HUNT
                ? "사냥 의뢰" + (objectives.size() > 1 ? " (" + objectives.size() + "종)" : "") : "아이템 납품";
        return new AutoQuestData(slot, "auto-" + slot + "-" + now, type, objectives, name,
                now, expires, rewardCoins, rewardExp, AutoQuestStatus.ACTIVE);
    }

    private boolean stillDefined(AutoQuestData quest) {
        for (AutoQuestObjectiveData objective : quest.objectives()) {
            boolean defined = switch (objective.source()) {
                case HYUNSEORPG_CUSTOM_MOB -> availability.checkMonster(null, objective.target(), MonsterEligibilityContext.ADMIN_VALIDATION).eligible();
                case VANILLA_MOB -> true;
                case HYUNSEORPG_CUSTOM_ITEM, VANILLA_ITEM -> availability.checkItem(null,
                        objective.source() == QuestTargetSource.VANILLA_ITEM ? "vanilla:" + objective.target() : objective.target(),
                        ItemObtainabilityContext.ADMIN_VALIDATION).obtainable();
            };
            if (!defined) return false;
        }
        return true;
    }

    private boolean matches(RPGMobKillEvent event, AutoQuestObjectiveData objective) {
        return switch (objective.source()) {
            case HYUNSEORPG_CUSTOM_MOB -> event.getContext().customMob()
                    && event.getContext().customMobId().equalsIgnoreCase(objective.target())
                    && isNaturalCustomSpawn(event.getContext().spawnSource());
            case VANILLA_MOB -> !event.getContext().customMob()
                    && event.getContext().vanillaType().name().equalsIgnoreCase(objective.target());
            default -> false;
        };
    }

    private boolean isNaturalCustomSpawn(String source) {
        return "replacement".equalsIgnoreCase(source) || "additive".equalsIgnoreCase(source);
    }

    private int firstFreeSlot(PlayerRPGData data) {
        for (int slot = 1; slot <= maxActive(); slot++) if (data.getAutoQuest(slot) == null) return slot;
        return -1;
    }

    private void fail(PlayerRPGData data, int slot, String cooldownKey) {
        AutoQuestData quest = data.getAutoQuest(slot);
        if (quest != null) {
            quest.setStatus(AutoQuestStatus.FAILED);
            data.incrementFailedAutoQuestCount();
        }
        data.removeAutoQuest(slot);
        setCooldown(data, cooldownKey);
    }

    private void setCooldown(PlayerRPGData data, String key) {
        long seconds = Math.max(0L, config.getQuestsLong("auto.cooldown." + key, 300L));
        data.setAutoQuestCooldownUntil(System.currentTimeMillis() + seconds * 1000L);
    }

    private int countMatchingItems(Player player, AutoQuestObjectiveData objective) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (matchesItem(item, objective)) total += item.getAmount();
        }
        return total;
    }

    private List<Removal> buildRemovalPlan(Player player, AutoQuestData quest) {
        List<Removal> plan = new ArrayList<>();
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (AutoQuestObjectiveData objective : quest.objectives()) {
            int remaining = objective.amount();
            for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
                ItemStack item = storage[slot];
                if (!matchesItem(item, objective)) continue;
                int amount = Math.min(remaining, item.getAmount());
                plan.add(new Removal(slot, amount, objective));
                remaining -= amount;
            }
        }
        return plan;
    }

    private boolean planStillValid(Player player, AutoQuestData quest, List<Removal> plan) {
        for (Removal removal : plan) {
            ItemStack current = player.getInventory().getItem(removal.slot());
            if (current == null || current.getAmount() < removal.amount() || !matchesItem(current, removal.objective())) return false;
        }
        return true;
    }

    private void applyRemovalPlan(Player player, List<Removal> plan) {
        for (Removal removal : plan) {
            ItemStack current = player.getInventory().getItem(removal.slot());
            if (current == null) continue;
            int next = current.getAmount() - removal.amount();
            if (next <= 0) player.getInventory().setItem(removal.slot(), null);
            else current.setAmount(next);
        }
    }

    private boolean planFulfillsObjectives(List<Removal> plan, AutoQuestData quest) {
        for (AutoQuestObjectiveData objective : quest.objectives()) {
            int planned = plan.stream().filter(removal -> removal.objective() == objective).mapToInt(Removal::amount).sum();
            if (planned < objective.amount()) return false;
        }
        return true;
    }

    private boolean matchesItem(ItemStack item, AutoQuestObjectiveData objective) {
        if (item == null || item.getType().isAir()) return false;
        return switch (objective.source()) {
            case HYUNSEORPG_CUSTOM_ITEM -> itemService.isItem(item, objective.target());
            case VANILLA_ITEM -> item.getType().name().equalsIgnoreCase(objective.target()) && itemService.getItemId(item).isEmpty();
            default -> false;
        };
    }

    private record Removal(int slot, int amount, AutoQuestObjectiveData objective) { }
}
