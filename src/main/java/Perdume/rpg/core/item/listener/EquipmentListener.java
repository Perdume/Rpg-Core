package Perdume.rpg.core.item.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.CustomItemUtil;
import Perdume.rpg.core.item.EnhancementManager;
import Perdume.rpg.core.item.ItemManager;
import Perdume.rpg.core.item.PotentialManager;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.item.ModifierType;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.stat.StatModifier;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.event.block.Action; // [디버그] import 추가

/**
 * [UUID 제거] UUID 대신 '장비 슬롯'을 기준으로 스탯을 적용하도록 로직 전면 수정
 * (체력 깜빡임 버그 자동 해결)
 */
public class EquipmentListener implements Listener {

    private final Rpg plugin;
    private final AuraSkillsApi auraSkillsApi;
    private final dev.aurelium.auraskills.api.item.ItemManager auraSkillsItemManager; // [AI] 2.x API
    private final ItemManager itemManager;
    private final EnhancementManager enhancementManager;
    private final PotentialManager potentialManager;


    public EquipmentListener(Rpg plugin) {
        this.plugin = plugin;
        this.auraSkillsApi = plugin.getAuraSkillsApi();
        // [AI] 2.x API
        this.auraSkillsItemManager = plugin.getAuraSkillsBukkit().getItemManager();
        this.itemManager = plugin.getItemManager();
        this.enhancementManager = plugin.getEnhancementManager();
        this.potentialManager = plugin.getPotentialManager();
    }

    // --- 스탯 적용/제거 이벤트 감지 ---
    // (이벤트 핸들러들은 수정할 필요 없음: onPlayerJoin, onPlayerQuit, onInventoryClick 등)
    // --- (생략) ---
    // 1. 접속 시
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateAllEquipmentStats(event.getPlayer());
        }, 1L);
    }

    // 2. 접속 종료 시
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeAllEquipmentStats(event.getPlayer());
    }

    // 3. 인벤토리 클릭 시
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateAllEquipmentStats((Player) event.getWhoClicked());
        }, 1L);
    }

    // 4. 아이템 버리기
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateAllEquipmentStats(event.getPlayer());
        }, 1L);
    }

    // 5. 손에 든 아이템 변경 (핫바 스크롤)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateAllEquipmentStats(event.getPlayer());
        }, 1L);
    }

    // 6. 스왑 (F키)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateAllEquipmentStats(event.getPlayer());
        }, 1L);
    }

    // 7. 우클릭 (방어구 착용)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {

        // (디버그 로직은 필요시 유지, 여기서는 제거된 버전)

        // --- [기존 로직] (디버그가 실행되지 않았을 경우에만 도달) ---
        if (event.getAction().name().startsWith("RIGHT_CLICK")) {
            // [디버그] 로그 삭제
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                updateAllEquipmentStats(event.getPlayer());
            }, 1L);
        }
    }

    // 8. 리스폰 (아이템을 다시 받을 때)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateAllEquipmentStats(event.getPlayer());
        }, 1L);
    }

    // 9. 아이템 줍기
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                updateAllEquipmentStats((Player) event.getEntity());
            }, 1L);
        }
    }

    // --- 핵심 로직 ---

    /**
     * [UUID 제거] "슬롯" 기준으로 스탯을 갱신하도록 로직 전면 수정
     * (체력 깜빡임 현상 자동 해결)
     */
    private void updateAllEquipmentStats(Player player) {
        if (player == null || !player.isOnline()) return;

        SkillsUser user = auraSkillsApi.getUser(player.getUniqueId());
        if (user == null) return; // AuraSkills 데이터가 아직 로드되지 않음

        // 1. 현재 "활성화되어야 할" 모든 스탯 모디파이어의 이름을 추적할 Set 생성
        Set<String> activeModifierNames = new HashSet<>();

        // 2. '착용 중인' 장비(방어구 + 손)만 스캔합니다.
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // 3. [슬롯 기반] 방어구 스탯 적용 및 모디파이어 이름 수집
        // (armor[3] = helmet, armor[2] = chestplate, ...)
        activeModifierNames.addAll(getStatsForSlot(user, armor[3], player, "helmet"));
        activeModifierNames.addAll(getStatsForSlot(user, armor[2], player, "chestplate"));
        activeModifierNames.addAll(getStatsForSlot(user, armor[1], player, "leggings"));
        activeModifierNames.addAll(getStatsForSlot(user, armor[0], player, "boots"));

        // 4. [슬롯 기반] 손에 든 아이템 스탯 적용 및 모디파이어 이름 수집
        activeModifierNames.addAll(getStatsForSlot(user, mainHand, player, "mainhand"));
        activeModifierNames.addAll(getStatsForSlot(user, offHand, player, "offhand"));

        // 5. 현재 플레이어에게 적용된 "rpg_item_" 모디파이어 중,
        //    방금 갱신한 목록(activeModifierNames)에 '없는' 모디파이어(즉, 빈 슬롯)를 찾습니다.
        Set<String> modifiersToRemove = new HashSet<>();
        for (StatModifier modifier : user.getStatModifiers().values()) {
            String modifierName = modifier.name();
            // 우리 플러그인이 적용한 모디파이어인지 확인
            if (modifierName.startsWith("rpg_item_")) {
                // 현재 활성화 목록에 이 모디파이어가 없다면
                if (!activeModifierNames.contains(modifierName)) {
                    modifiersToRemove.add(modifierName);
                }
            }
        }

        // 6. '활성화 목록에 없는' (오래된/빈 슬롯) 모디파이어만 제거합니다.
        for (String modifierName : modifiersToRemove) {
            user.removeStatModifier(modifierName);
        }
    }

    /**
     * [UUID 제거] 플레이어에게서 모든 아이템 스탯 모디파이어를 제거합니다. (로그아웃 시)
     */
    private void removeAllEquipmentStats(Player player) {
        if (player == null) return;
        SkillsUser user = auraSkillsApi.getUser(player.getUniqueId());
        if (user == null) return;

        Set<String> modifiersToRemove = new HashSet<>();
        for (StatModifier modifier : user.getStatModifiers().values()) {
            if (modifier.name().startsWith("rpg_item_")) {
                modifiersToRemove.add(modifier.name());
            }
        }
        for (String modifierName : modifiersToRemove) {
            user.removeStatModifier(modifierName);
        }
    }

    /**
     * [UUID 제거] `applyStats` 메소드를 대체하는 새로운 헬퍼 메소드
     * (1) 아이템의 요구조건을 검사하고,
     * (2) [수정됨] '템플릿'에서 기본 스탯을, '인스턴스'에서 강화/잠재 스탯을 읽어와 합산
     * (3) '슬롯' 기반 모디파이어를 적용하고, 적용된 모디파이어 이름 Set을 반환
     */
    private Set<String> getStatsForSlot(SkillsUser user, ItemStack item, Player player, String slotName) {
        Set<String> appliedModifiers = new HashSet<>();

        // 1. 아이템이 유효한지, 요구 조건을 통과하는지 검사
        if (item == null || item.getType().isAir() || !checkRequirements(user, item, player)) {
            // 아이템이 없거나 요구조건 미달시, 이 슬롯에 적용된 스탯이 없어야 함
            return appliedModifiers; // 빈 Set 반환 (-> updateAllEquipmentStats가 이 슬롯의 모든 스탯을 제거함)
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return appliedModifiers;

        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        String itemId = nbt.get(ItemManager.CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        if (itemId == null) {
            return appliedModifiers; // 커스텀 아이템이 아님
        }

        // 2. [버전 자동 갱신] 기본 스탯을 'ItemManager (템플릿)'에서 가져옴
        // [핵심] 이 메소드는 이제 AuraSkills NBT 대신 우리 고유 PDC(CUSTOM_BASE_STATS_KEY)를 읽음
        Map<Stat, Double> baseStats = itemManager.getBaseStats(itemId);

        // 3. [강화값] 스탯을 '아이템 인스턴스 NBT'에서 가져옴
        Map<Stat, Double> enhanceStats = enhancementManager.getEnhanceStats(nbt);

        // 4. [큐브값] 스탯을 '아이템 인스턴스 NBT'에서 가져옴
        Map<Stat, Double> potentialStats = potentialManager.getPotentialStats(nbt);

        // 5. 모든 스탯을 'totalStats' 맵으로 합산합니다.
        Map<Stat, Double> totalStats = new HashMap<>(baseStats);
        enhanceStats.forEach((stat, value) -> totalStats.merge(stat, value, Double::sum));
        potentialStats.forEach((stat, value) -> totalStats.merge(stat, value, Double::sum));

        // 6. 합산된 스탯을 '슬롯' 기반 모디파이어로 적용
        for (Map.Entry<Stat, Double> entry : totalStats.entrySet()) {
            Stat stat = entry.getKey();
            double value = entry.getValue();

            if (value == 0) continue; // 0은 적용할 필요 없음

            // [UUID 제거] 모디파이어 이름: "rpg_item_[슬롯이름]_[스탯ID]" (예: "rpg_item_helmet_health")
            String modifierName = "rpg_item_" + slotName + "_" + stat.name().toLowerCase(Locale.ROOT);

            // AuraSkills에 스탯 적용 (업데이트 또는 추가)
            user.addStatModifier(new StatModifier(modifierName, stat, value));

            // 적용된 모디파이어 이름을 Set에 추가
            appliedModifiers.add(modifierName);
        }

        // 적용된 모디파이어 이름 Set을 반환
        return appliedModifiers;
    }

    /**
     * (Helper) 플레이어가 이 아이템의 요구 조건을 충족하는지 검사합니다.
     * (이 메소드는 수정할 필요 없음)
     */
    private boolean checkRequirements(SkillsUser user, ItemStack item, Player player) {
        if (!item.hasItemMeta()) return true;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        // 1. AuraSkills NBT에서 스킬 요구사항 읽기 (AuraSkills 스킬 레벨)
        Map<Skill, Integer> auraSkillRequirements = auraSkillsItemManager.getRequirements(item, ModifierType.ITEM);
        if (!auraSkillRequirements.isEmpty()) {
            for (Map.Entry<Skill, Integer> entry : auraSkillRequirements.entrySet()) {
                Skill req = entry.getKey();
                int level = entry.getValue();
                if (user.getSkillLevel((Skill) req) < level) {
                    return false; // AuraSkills 스킬 레벨 부족
                }
            }
        }

        // 2. 우리 고유 NBT에서 커스텀 요구사항 읽기 (RPG 레벨, 스탯)
        if (nbt.has(ItemManager.CUSTOM_REQUIREMENTS_KEY, PersistentDataType.TAG_CONTAINER)) {
            PersistentDataContainer customReqNbt = nbt.get(ItemManager.CUSTOM_REQUIREMENTS_KEY, PersistentDataType.TAG_CONTAINER);
            if (customReqNbt == null) return true; // 커스텀 NBT가 비어있으면 통과

            for (NamespacedKey key : customReqNbt.getKeys()) {
                String keyName = key.getKey();

                // 2-1. RPG 레벨 요구 조건 확인
                if (keyName.equals("level")) {
                    int requiredLevel = customReqNbt.getOrDefault(key, PersistentDataType.INTEGER, 0);
                    // PlayerDataManager를 통해 플레이어의 실제 RPG 레벨을 가져옴
                    int playerLevel = plugin.getPlayerDataManager().getPlayerData(player).getStats().getLevel();
                    if (playerLevel < requiredLevel) {
                        return false; // RPG 레벨 부족
                    }
                }
                // 2-2. RPG 스탯 요구 조건 확인
                else {
                    Stat stat = itemManager.getStatFromString(keyName);
                    if (stat != null) {
                        int requiredStat = customReqNbt.getOrDefault(key, PersistentDataType.INTEGER, 0);
                        // AuraSkills의 '총 스탯'을 기준으로 검사
                        if (user.getStatLevel(stat) < requiredStat) {
                            return false; // RPG 스탯 부족
                        }
                    }
                }
            }
        }

        return true; // 모든 요구 조건 통과
    }
}