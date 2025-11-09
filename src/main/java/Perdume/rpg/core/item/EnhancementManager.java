package Perdume.rpg.core.item;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.stats.RpgCustomStats;
import Perdume.rpg.core.util.ItemFactory;
import dev.aurelium.auraskills.api.item.ModifierType;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.stat.Stats;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import dev.aurelium.auraskills.api.stat.StatModifier;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * [수정됨 - 2.x API 및 1.21+ Bukkit API 호환]
 * 'Reinforce.yml'을 로드하고,
 * 아이템 강화 로직(NBT 수정, Lore 갱신)을 처리하는 관리자입니다.
 */
public class EnhancementManager {

    private final Rpg plugin;
    private final ItemManager itemManager;
    // [AI] 4단계: Lore 렌더링을 위해 PotentialManager 참조
    private final PotentialManager potentialManager;

    // --- NBT 키 정의 (ItemManager와 공유) ---
    public static NamespacedKey ENHANCE_LEVEL_KEY = ItemManager.ENHANCE_LEVEL_KEY;
    public static NamespacedKey ENHANCE_STATS_KEY = ItemManager.ENHANCE_STATS_KEY;
    public static NamespacedKey ITEM_TYPE_KEY = ItemManager.ITEM_TYPE_KEY;


    // --- YML 캐시 ---
    private File reinforceFile;
    private FileConfiguration reinforceConfig;
    private String catalystItemType; // [AI] ID -> Type

    public EnhancementManager(Rpg plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        // [AI] Rpg.java에서 초기화된 인스턴스를 가져옴 (Null-safe)
        this.potentialManager = plugin.getPotentialManager();

        // [AI] 파일명 변경
        this.reinforceFile = new File(plugin.getDataFolder(), "Reinforce.yml");
        if (!reinforceFile.exists()) {
            plugin.saveResource("Reinforce.yml", false);
        }
        loadConfig();
    }

    public void loadConfig() {
        reinforceConfig = YamlConfiguration.loadConfiguration(reinforceFile);

        // [AI] ID 대신 item-type을 읽어옴 (items.yml의 "enhancement_stone"은 "catalyst" 타입이어야 함)
        catalystItemType = reinforceConfig.getString("catalyst.item-type", "catalyst");
        Rpg.log.info("Loaded enhancement catalyst type: " + catalystItemType);
    }

    /**
     * [AI MODIFIED] "검을 인식 못하는 버그" 수정
     * itemTypeMap 대신 NBT(PDC)의 ITEM_TYPE_KEY를 읽어 확인합니다.
     */
    public boolean isEnhanceable(ItemStack item) {
        Rpg.log.info("--- [디버그/강화] isEnhanceable 검사 시작 ---"); // 디버그 로그

        if (item == null || item.getType().isAir()) {
            Rpg.log.info("[디버그/강화] 1. 아이템이 null이거나 공기입니다. [검사 실패]");
            return false;
        }
        if (!item.hasItemMeta()) {
            Rpg.log.info("[디버그/강화] 1. 아이템에 ItemMeta가 없습니다. (아이템: " + item.getType() + ") [검사 실패]");
            return false;
        }
        PersistentDataContainer nbt = item.getItemMeta().getPersistentDataContainer();
        String itemType = nbt.get(ITEM_TYPE_KEY, PersistentDataType.STRING);

        // [핵심 디버그 로그]
        if (itemType == null) {
            Rpg.log.warning("[디버그/강화] 2. [심각] 아이템 NBT에서 'custom-item-type'을 찾을 수 없습니다!");
            Rpg.log.warning("[디버그/강화]    -> 아이템: " + item.getItemMeta().getDisplayName());
            Rpg.log.warning("[디버그/강화]    -> NBT 키 목록: " + nbt.getKeys().toString());
            Rpg.log.warning("[디버그/강화]    (이전 답변의 ItemManager.java 버그가 원인일 수 있습니다) [검사 실패]");
            return false;
        }

        Rpg.log.info("[디버그/강화] 2. 아이템 타입 NBT = " + itemType);

        if (itemType.equals(this.catalystItemType)) {
            Rpg.log.info("[디버그/강화] 3. 아이템이 재료(" + itemType + ")이므로 [검사 실패]");
            return false;
        }

        boolean tableExists = reinforceConfig.contains("tables." + itemType);
        Rpg.log.info("[디버그/강화] 3. Reinforce.yml에 'tables." + itemType + "' 테이블 존재 여부: " + tableExists);
        if(tableExists) Rpg.log.info("--- [디버그/강화] isEnhanceable [검사 통과] ---");
        else Rpg.log.info("--- [디버그/강화] isEnhanceable [검사 실패] ---");
        return tableExists;
    }

    /**
     * [AI MODIFIED] "재료를 인식 못하는 버그" 수정
     * ID 대신 NBT(PDC)의 ITEM_TYPE_KEY를 읽어 확인합니다.
     */
    public boolean isCatalyst(ItemStack item) {
        Rpg.log.info("--- [디버그/강화] isCatalyst 검사 시작 ---"); // 디버그 로그

        if (item == null || item.getType().isAir()) {
            Rpg.log.info("[디버그/강화] 1. 재료가 null이거나 공기입니다. [검사 실패]");
            return false;
        }
        if (!item.hasItemMeta()) {
            Rpg.log.info("[디버그/강화] 1. 재료에 ItemMeta가 없습니다. (아이템: " + item.getType() + ") [검사 실패]");
            return false;
        }
        PersistentDataContainer nbt = item.getItemMeta().getPersistentDataContainer();
        String itemType = nbt.get(ITEM_TYPE_KEY, PersistentDataType.STRING);

        // [핵심 디버그 로그]
        if (itemType == null) {
            Rpg.log.warning("[디버그/강화] 2. [심각] 재료 NBT에서 'custom-item-type'을 찾을 수 없습니다!");
            Rpg.log.warning("[디버그/강화]    -> 아이템: " + item.getItemMeta().getDisplayName());
            Rpg.log.warning("[디버그/강화]    -> NBT 키 목록: " + nbt.getKeys().toString());
            return false;
        }

        Rpg.log.info("[디버그/강화] 2. 재료 타입 NBT = " + itemType);
        boolean isCatalyst = itemType.equals(this.catalystItemType);
        Rpg.log.info("[디버그/강화] 3. 설정된 재료 타입(" + this.catalystItemType + ")과 일치 여부: " + isCatalyst);

        if(isCatalyst) Rpg.log.info("--- [디버그/강화] isCatalyst [검사 통과] ---");
        else Rpg.log.info("--- [디버그/강화] isCatalyst [검사 실패] ---");
        return isCatalyst;
    }

    public int getEnhanceLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer nbt = item.getItemMeta().getPersistentDataContainer();
        return nbt.getOrDefault(ENHANCE_LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }

    /**
     * [핵심 로직] [AI MODIFIED] 1.21+ NBT API, Reinforce.yml
     * [최종 버그 수정] item.setItemMeta() 호출 시 유실되는 AuraSkills 기본 스탯 NBT를 '재적용'
     */
    public EnhanceResult enhanceItem(ItemStack item) {
        String itemId = CustomItemUtil.getItemId(item);

        if (item == null || !item.hasItemMeta()) return EnhanceResult.FAIL;
        ItemMeta meta = item.getItemMeta(); // Meta를 먼저 가져옴
        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        String itemType = nbt.get(ITEM_TYPE_KEY, PersistentDataType.STRING);

        if (itemId == null || itemType == null) return EnhanceResult.FAIL;

        int currentLevel = getEnhanceLevel(item);

        ConfigurationSection table = reinforceConfig.getConfigurationSection("tables." + itemType + "." + currentLevel);
        if (table == null) {
            Rpg.log.warning("'" + itemType + "'의 " + currentLevel + "강 강화 테이블을 Reinforce.yml에서 찾을 수 없습니다.");
            return EnhanceResult.FAIL; // 최대 레벨
        }

        double successChance = table.getDouble("success_chance", 0.0);
        double failChance = table.getDouble("fail_chance", 0.0);
        double destroyChance = table.getDouble("destroy_chance", 0.0);
        double roll = new Random().nextDouble() * 100.0;

        if (roll < destroyChance) {
            // 1. 파괴
            item.setAmount(0);
            return EnhanceResult.DESTROY;

        } else if (roll < (destroyChance + successChance)) {
            // 2. 성공
            int newLevel = currentLevel + 1;

            nbt.set(ENHANCE_LEVEL_KEY, PersistentDataType.INTEGER, newLevel);

            PersistentDataContainer statsNbt;
            if (nbt.has(ENHANCE_STATS_KEY, PersistentDataType.TAG_CONTAINER)) {
                statsNbt = nbt.get(ENHANCE_STATS_KEY, PersistentDataType.TAG_CONTAINER);
            } else {
                statsNbt = meta.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
            }

            ConfigurationSection statsSection = table.getConfigurationSection("stats");
            if (statsSection != null && statsNbt != null) {
                for (String statId : statsSection.getKeys(false)) {
                    NamespacedKey statKey = new NamespacedKey(plugin, statId.toLowerCase(Locale.ROOT));
                    double bonusValue = statsSection.getDouble(statId);
                    double currentValue = statsNbt.getOrDefault(statKey, PersistentDataType.DOUBLE, 0.0);
                    statsNbt.set(statKey, PersistentDataType.DOUBLE, currentValue + bonusValue);
                }
            }
            nbt.set(ENHANCE_STATS_KEY, PersistentDataType.TAG_CONTAINER, statsNbt);

            // 1. 로어 갱신 (아직 저장 안 함)
            rerenderItemLore(item, itemId, newLevel, meta);

            // 2. [버그 발생 지점] PDC가 수정된 meta를 저장 (AuraSkills NBT 삭제됨)
            item.setItemMeta(meta);

            // 3. [버그 해결] 삭제된 AuraSkills 기본 스탯 NBT를 '재적용'
            //    (ItemManager에서 템플릿을 가져와서 재적용)
            dev.aurelium.auraskills.api.item.ItemManager auraSkillsItemManager = plugin.getAuraSkillsBukkit().getItemManager();
            Map<Stat, Double> baseStats = itemManager.getBaseStats(itemId);

            for (Map.Entry<Stat, Double> entry : baseStats.entrySet()) {
                auraSkillsItemManager.addStatModifier(item, ModifierType.ITEM, entry.getKey(), entry.getValue(), false);
            }

            return EnhanceResult.SUCCESS;

        } else {
            // 3. 실패 (파괴도 아니고 성공도 아님)
            return EnhanceResult.FAIL;
        }
    }
    /**
     * [신규] [AI MODIFIED] Reinforce.yml 참조
     */
    public boolean isMaxLevel(ItemStack item, int currentLevel) {
        if (item == null || !item.hasItemMeta()) return true;
        String itemType = item.getItemMeta().getPersistentDataContainer().get(ITEM_TYPE_KEY, PersistentDataType.STRING);
        if (itemType == null) return true; // 강화 불가

        return reinforceConfig.getConfigurationSection("tables." + itemType + "." + currentLevel) == null;
    }

    /**
     * [신규] [AI MODIFIED] Reinforce.yml 참조
     */
    public String getChanceLore(ItemStack item, int currentLevel) {
        if (item == null || !item.hasItemMeta()) return "§c(알 수 없는 타입)";
        String itemType = item.getItemMeta().getPersistentDataContainer().get(ITEM_TYPE_KEY, PersistentDataType.STRING);
        if (itemType == null) return "§c(알 수 없는 타입)";

        ConfigurationSection table = reinforceConfig.getConfigurationSection("tables." + itemType + "." + currentLevel);
        if (table == null) {
            return "§c(알 수 없는 확률)";
        }

        double success = table.getDouble("success_chance", 0.0);
        double fail = table.getDouble("fail_chance", 0.0);
        double destroy = table.getDouble("destroy_chance", 0.0);

        if (success >= 100.0 && fail == 0.0 && destroy == 0.0) {
            return "§a(100% 성공)";
        }

        return String.format("§a성공: %.1f%% §7/ §e실패: %.1f%% §7/ §c파괴: %.1f%%", success, fail, destroy);
    }

    /**
     * [AI MODIFIED] 4단계: '잠재능력' Lore를 포함하도록 수정
     * [버그 수정] meta 객체를 파라미터로 받아, setItemMeta 호출을 enhanceItem으로 위임
     * [최종 버그 수정] NBT 유실을 막기 위해, 'CUSTOM_BASE_STATS_KEY'에 백업된 고유 PDC에서 기본 스탯을 읽어옵니다.
     * [사용자 요청] (기본+강화+큐브) 상세 로어를 항상 표시하도록 수정
     */
    public void rerenderItemLore(ItemStack item, String itemId, int enhanceLevel, ItemMeta itemMeta) {
        if (item == null || potentialManager == null || itemMeta == null) return;

        ItemStack template = itemManager.getItem(itemId); // 템플릿 (AuraSkills NBT 포함)
        if (template == null) return;

        ItemMeta templateMeta = template.getItemMeta();
        PersistentDataContainer itemNbt = itemMeta.getPersistentDataContainer();

        String baseName = templateMeta.getDisplayName();
        if (enhanceLevel > 0) {
            itemMeta.setDisplayName("§e+" + enhanceLevel + " " + baseName);
        } else {
            itemMeta.setDisplayName(baseName);
        }

        List<String> newLore = new ArrayList<>();

        // 1. 템플릿에서 '기본 Lore' (스탯/요구조건 제외)를 가져옴
        List<String> baseLore = templateMeta.getLore();
        if (baseLore != null) {
            for (String line : baseLore) {
                // [AI] 2.x API는 Lore를 수동 생성하므로, 구분선을 직접 찾아야 함
                if (line.contains("§7--------------------")) break;
                newLore.add(line);
            }
        }

        // 2. 스탯 합산 (기본 + 강화 + 잠재)

        // [버그 1 수정] 'CUSTOM_BASE_STATS_KEY' NBT에서 기본 스탯을 읽어옵니다.
        Map<Stat, Double> baseStats = new HashMap<>();
        if (itemNbt.has(ItemManager.CUSTOM_BASE_STATS_KEY, PersistentDataType.TAG_CONTAINER)) {
            PersistentDataContainer baseStatsNbt = itemNbt.get(ItemManager.CUSTOM_BASE_STATS_KEY, PersistentDataType.TAG_CONTAINER);
            if (baseStatsNbt != null) {
                for (NamespacedKey key : baseStatsNbt.getKeys()) {
                    Stat stat = itemManager.getStatFromString(key.getKey());
                    if (stat != null) {
                        baseStats.put(stat, baseStatsNbt.getOrDefault(key, PersistentDataType.DOUBLE, 0.0));
                    }
                }
            }
        }

        Map<Stat, Double> enhanceStats = getEnhanceStats(itemNbt);
        Map<Stat, Double> potentialStats = potentialManager.getPotentialStats(itemNbt);

        Map<Stat, Double> totalStats = new HashMap<>(baseStats);
        enhanceStats.forEach((stat, value) -> totalStats.merge(stat, value, Double::sum));
        potentialStats.forEach((stat, value) -> totalStats.merge(stat, value, Double::sum));

        if (!totalStats.isEmpty()) {
            newLore.add("§7--------------------");
            newLore.add("§a[능력치]");
            for (Map.Entry<Stat, Double> entry : totalStats.entrySet()) {
                Stat stat = entry.getKey();
                double value = entry.getValue();

                // [버그 수정] totalStats에 있지만 baseStats에는 없는 스탯(e.g. 힘)을 위해 0.0으로 기본값 처리
                double b = baseStats.getOrDefault(stat, 0.0);
                double e = enhanceStats.getOrDefault(stat, 0.0);
                double p = potentialStats.getOrDefault(stat, 0.0);

                String detail = "";

                // [사용자 요청 수정] 총 스탯이 0이 아닌 경우, 항상 상세 로어를 표시
                if (value != 0) {
                    detail = String.format(" §7(%.0f §e+%.0f §b+%.0f§7)", b, e, p);
                }

                String statName = getStatDisplayName(stat);

                // [사용자 요청 수정] 총 스탯이 0이 아닌 경우에만 로어에 추가
                if (value != 0) {
                    newLore.add(String.format("§7- %s: §e+%.1f%s", statName, value, detail));
                }
            }
        }

        // 3. 템플릿에서 '요구 조건 Lore'를 다시 가져와서 추가
        if (baseLore != null) {
            boolean foundReq = false;
            for (String line : baseLore) {
                if (foundReq) {
                    newLore.add(line);
                }
                // [AI] 2.x API는 Lore를 수동 생성하므로, "착용 조건" 문자열로 찾음
                if (line.contains("§c[착용 조건]")) {
                    newLore.add("§7--------------------");
                    newLore.add(line);
                    foundReq = true;
                }
            }
        }

        // 4. [AI] 4-D: 잠재능력 Lore 추가
        String gradeDisplay = potentialManager.getGradeDisplayName(itemNbt);
        newLore.add("§7--------------------");
        newLore.add(gradeDisplay); // 예: "§9[Rare]"
        if (potentialStats.isEmpty()) {
            newLore.add("§7(옵션 미설정)");
        } else {
            for (Map.Entry<Stat, Double> entry : potentialStats.entrySet()) {
                // TODO: % 스탯과 플랫 스탯을 구분하여 표시 (PotentialManager와 연동 필요)
                String statName = getStatDisplayName(entry.getKey());
                newLore.add(String.format("§7- %s: §b+%.1f", statName, entry.getValue()));
            }
        }

        itemMeta.setLore(newLore);
        // [버그 수정] item.setItemMeta(itemMeta); (삭제) -> 호출한 enhanceItem에서 한 번만 저장
    }


    /**
     * [수정됨] 1.21+ API (adapterContext) 오류를 피하기 위해 has/get을 사용하는 NBT 리더
     */
    public Map<Stat, Double> getEnhanceStats(PersistentDataContainer nbt) {
        Map<Stat, Double> enhanceStats = new HashMap<>();

        if (!nbt.has(ENHANCE_STATS_KEY, PersistentDataType.TAG_CONTAINER)) {
            return enhanceStats;
        }

        PersistentDataContainer statsNbt = nbt.get(ENHANCE_STATS_KEY, PersistentDataType.TAG_CONTAINER);
        if (statsNbt == null) return enhanceStats;

        for (NamespacedKey key : statsNbt.getKeys()) {
            Stat stat = itemManager.getStatFromString(key.getKey());
            if (stat != null) {
                enhanceStats.put(stat, statsNbt.getOrDefault(key, PersistentDataType.DOUBLE, 0.0));
            }
        }
        return enhanceStats;
    }

    /**
     * [신규 헬퍼] [AI] 2.x API 호환
     */
    private String getStatDisplayName(Stat stat) {
        if (stat.equals(RpgCustomStats.PHYSICAL_POWER)) { return "§c물리 공격력"; }
        if (stat.equals(RpgCustomStats.MAGIC_POWER)) { return "§b마법 공격력"; }
        if (stat.equals(Stats.STRENGTH)) { return "§4힘"; }
        if (stat.equals(Stats.HEALTH)) { return "§c체력"; }
        if (stat.equals(Stats.WISDOM)) { return "§9지혜"; }
        if (stat.equals(Stats.TOUGHNESS)) { return "§8방어력"; }
        // (나머지 스탯 추가)
        return stat.name(); // (최후의 수단: "STRENGTH")
    }

    public enum EnhanceResult {
        SUCCESS,
        FAIL,
        DESTROY,
        NO_CATALYST,
        NO_ITEM
    }

    public Rpg getPlugin() {
        return this.plugin;
    }
}