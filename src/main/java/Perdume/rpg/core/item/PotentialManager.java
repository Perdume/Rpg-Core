package Perdume.rpg.core.item;

import Perdume.rpg.Rpg;
import dev.aurelium.auraskills.api.item.ModifierType;
import dev.aurelium.auraskills.api.stat.Stat;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * [신규] 4단계: 커스텀 인챈트 (큐브/잠재능력) 시스템
 * 'enhancement.yml' (큐브 파일)을 읽고, 아이템의 잠재능력 옵션을 관리합니다.
 */
public class PotentialManager {

    private final Rpg plugin;
    private final ItemManager itemManager;

    // --- NBT 키 (ItemManager와 공유) ---
    public static NamespacedKey ITEM_TYPE_KEY = ItemManager.ITEM_TYPE_KEY;
    public static NamespacedKey POTENTIAL_GRADE_KEY = ItemManager.POTENTIAL_GRADE_KEY;
    public static NamespacedKey POTENTIAL_STATS_KEY = ItemManager.POTENTIAL_STATS_KEY;

    // --- YML 캐시 ---
    private File potentialFile;
    private FileConfiguration potentialConfig;
    
    // 큐브 정보
    private final Map<String, Map<String, Object>> cubeRegistry = new HashMap<>();
    // 등급 정보 (순서가 중요하므로 LinkedHashMap)
    private final List<String> gradeOrder = new ArrayList<>();
    private final Map<String, Map<String, Object>> gradeRegistry = new HashMap<>();
    // 옵션 풀 정보 (ItemType -> Grade -> List<Options>)
    private final Map<String, Map<String, List<PotentialOption>>> optionPools = new HashMap<>();

    // 큐브/잠재능력 전용 옵션 객체
    private record PotentialOption(Stat stat, String type, double min, double max, double weight) {}

    public PotentialManager(Rpg plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        
        // [중요] 인수인계 문서에 따라, 'enhancement.yml'을 큐브 파일로 간주합니다.
        this.potentialFile = new File(plugin.getDataFolder(), "enhancement.yml");
        if (!potentialFile.exists()) {
            plugin.saveResource("enhancement.yml", false);
        }
        loadConfig();
    }

    public void loadConfig() {
        potentialConfig = YamlConfiguration.loadConfiguration(potentialFile);
        
        cubeRegistry.clear();
        gradeRegistry.clear();
        gradeOrder.clear();
        optionPools.clear();

        // 1. 큐브 로드 (cubes:)
        ConfigurationSection cubeSection = potentialConfig.getConfigurationSection("cubes");
        if (cubeSection != null) {
            for (String cubeId : cubeSection.getKeys(false)) {
                Map<String, Object> cubeData = new HashMap<>();
                cubeData.put("item-id", cubeSection.getString(cubeId + ".item-id"));
                cubeData.put("can_rank_up", cubeSection.getBoolean(cubeId + ".can_rank_up", false));
                cubeRegistry.put(cubeId, cubeData);
            }
            Rpg.log.info("Loaded " + cubeRegistry.size() + " cube types.");
        }

        // 2. 등급 로드 (grades:)
        ConfigurationSection gradeSection = potentialConfig.getConfigurationSection("grades");
        if (gradeSection != null) {
            // [중요] YML에 정의된 순서대로 등급을 로드
            for (String gradeId : gradeSection.getKeys(false)) {
                gradeOrder.add(gradeId); // "RARE", "EPIC", ...
                Map<String, Object> gradeData = new HashMap<>();
                gradeData.put("display", gradeSection.getString(gradeId + ".display", "§f[" + gradeId + "]"));
                gradeData.put("lines", gradeSection.getInt(gradeId + ".lines", 2));
                // 등급업 확률 (cubeId -> chance)
                gradeData.put("rank_up_chance", gradeSection.getConfigurationSection(gradeId + ".rank_up_chance").getValues(false));
                gradeRegistry.put(gradeId, gradeData);
            }
            Rpg.log.info("Loaded " + gradeRegistry.size() + " potential grades: " + String.join(" -> ", gradeOrder));
        }

        // 3. 옵션 풀 로드 (option_pools:)
        ConfigurationSection poolSection = potentialConfig.getConfigurationSection("option_pools");
        if (poolSection != null) {
            for (String itemType : poolSection.getKeys(false)) { // "weapon", "armor"
                ConfigurationSection typePool = poolSection.getConfigurationSection(itemType);
                Map<String, List<PotentialOption>> gradePoolMap = new HashMap<>();

                for (String gradeId : typePool.getKeys(false)) { // "RARE", "EPIC"
                    List<PotentialOption> options = new ArrayList<>();
                    List<Map<?, ?>> optionList = typePool.getMapList(gradeId);
                    
                    for (Map<?, ?> optionData : optionList) {
                        try {
                            Stat stat = itemManager.getStatFromString((String) optionData.get("stat"));
                            String type = (String) optionData.get("type");
                            double min = ((Number) optionData.get("min")).doubleValue();
                            double max = ((Number) optionData.get("max")).doubleValue();
                            double weight = ((Number) optionData.get("weight")).doubleValue();
                            if (stat != null) {
                                options.add(new PotentialOption(stat, type, min, max, weight));
                            }
                        } catch (Exception e) {
                            Rpg.log.warning("Failed to load potential option in " + itemType + "/" + gradeId + ": " + e.getMessage());
                        }
                    }
                    gradePoolMap.put(gradeId, options);
                }
                optionPools.put(itemType, gradePoolMap);
            }
            Rpg.log.info("Loaded " + optionPools.size() + " item-type option pools.");
        }
    }

    /**
     * NBT에서 아이템의 현재 잠재능력 등급을 읽어옵니다.
     */
    public String getPotentialGrade(PersistentDataContainer nbt) {
        return nbt.getOrDefault(POTENTIAL_GRADE_KEY, PersistentDataType.STRING, "RARE");
    }
    
    /**
     * NBT에서 등급 ID로 표시 이름을 가져옵니다.
     */
    public String getGradeDisplayName(PersistentDataContainer nbt) {
        String gradeId = getPotentialGrade(nbt);
        Map<String, Object> gradeData = gradeRegistry.get(gradeId);
        if (gradeData != null) {
            return (String) gradeData.get("display");
        }
        return "§f[등급 없음]";
    }

    /**
     * NBT(PDC)에서 잠재능력으로 추가된 스탯 맵을 읽어옵니다.
     * (EquipmentListener가 호출)
     */
    public Map<Stat, Double> getPotentialStats(PersistentDataContainer nbt) {
        Map<Stat, Double> potentialStats = new HashMap<>();

        if (!nbt.has(POTENTIAL_STATS_KEY, PersistentDataType.TAG_CONTAINER)) {
            return potentialStats;
        }

        PersistentDataContainer statsNbt = nbt.get(POTENTIAL_STATS_KEY, PersistentDataType.TAG_CONTAINER);
        if (statsNbt == null) return potentialStats;

        for (NamespacedKey key : statsNbt.getKeys()) {
            // NBT 키(예: "physical_power")로 Stat 객체를 찾음
            Stat stat = itemManager.getStatFromString(key.getKey());
            if (stat != null) {
                potentialStats.put(stat, statsNbt.getOrDefault(key, PersistentDataType.DOUBLE, 0.0));
            }
        }
        return potentialStats;
    }

    /**
     * [미완성] 큐브 사용의 핵심 로직입니다.
     * TODO: Black Cube (선택) 로직, GUI 연동
     * [최종 버그 수정] item.setItemMeta() 호출 시 유실되는 AuraSkills 기본 스탯 NBT를 '재적용'
     */
    public boolean applyCube(ItemStack item, ItemStack cube) {
        if (item == null || cube == null) return false;

        String cubeItemId = CustomItemUtil.getItemId(cube);
        String cubeId = null; // "red_cube", "black_cube"

        // 1. 사용된 큐브가 유효한지 확인
        for (Map.Entry<String, Map<String, Object>> entry : cubeRegistry.entrySet()) {
            if (entry.getValue().get("item-id").equals(cubeItemId)) {
                cubeId = entry.getKey();
                break;
            }
        }
        if (cubeId == null) return false; // 큐브가 아님
        Map<String, Object> cubeData = cubeRegistry.get(cubeId);

        // 2. 아이템 정보 읽기
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer nbt = meta.getPersistentDataContainer();
        String itemType = nbt.get(ITEM_TYPE_KEY, PersistentDataType.STRING);
        if (itemType == null) return false; // 잠재능력을 부여할 수 없는 아이템

        String currentGrade = getPotentialGrade(nbt);
        String newGrade = currentGrade;
        String itemId = CustomItemUtil.getItemId(item); // [버그 수정] itemId 가져오기

        // 3. 등급업 시도 (최대 등급이 아닐 경우)
        if ((boolean) cubeData.get("can_rank_up") && !currentGrade.equals(gradeOrder.get(gradeOrder.size() - 1))) {
            Map<String, Object> gradeData = gradeRegistry.get(currentGrade);
            Map<String, Object> rankUpChances = (Map<String, Object>) gradeData.get("rank_up_chance");
            double chance = ((Number) rankUpChances.getOrDefault(cubeId, 0.0)).doubleValue();

            if (ThreadLocalRandom.current().nextDouble(100.0) < chance) {
                // 등급업 성공!
                int nextGradeIndex = gradeOrder.indexOf(currentGrade) + 1;
                newGrade = gradeOrder.get(nextGradeIndex);
            }
        }

        // 4. 새로운 등급(newGrade)으로 NBT 설정
        nbt.set(POTENTIAL_GRADE_KEY, PersistentDataType.STRING, newGrade);

        // 5. 옵션 재설정 (Weighted Random)
        Map<String, Object> newGradeData = gradeRegistry.get(newGrade);
        int lineCount = (int) newGradeData.get("lines");
        List<PotentialOption> pool = optionPools.get(itemType).get(newGrade);

        if (pool == null || pool.isEmpty()) {
            Rpg.log.warning("'" + itemType + "'/" + newGrade + " 등급의 옵션 풀이 비어있습니다!");
            return false;
        }

        double totalWeight = pool.stream().mapToDouble(PotentialOption::weight).sum();
        Map<Stat, Double> newStats = new HashMap<>();

        // 6. 스탯 NBT 컨테이너를 '초기화'
        PersistentDataContainer statsNbt = nbt.getAdapterContext().newPersistentDataContainer();

        for (int i = 0; i < lineCount; i++) {
            double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
            PotentialOption chosenOption = null;
            for (PotentialOption option : pool) {
                roll -= option.weight();
                if (roll <= 0) {
                    chosenOption = option;
                    break;
                }
            }
            if (chosenOption == null) chosenOption = pool.get(pool.size() - 1); // (Fallback)

            // 옵션 값 랜덤 설정 (min ~ max)
            double value = chosenOption.min() + (chosenOption.max() - chosenOption.min()) * ThreadLocalRandom.current().nextDouble();

            NamespacedKey statKey = new NamespacedKey(plugin, chosenOption.stat().name().toLowerCase(Locale.ROOT));
            double currentValue = statsNbt.getOrDefault(statKey, PersistentDataType.DOUBLE, 0.0);
            statsNbt.set(statKey, PersistentDataType.DOUBLE, currentValue + value);
        }

        nbt.set(POTENTIAL_STATS_KEY, PersistentDataType.TAG_CONTAINER, statsNbt);

        // 7. Lore 갱신 (아직 저장 안 함)
        plugin.getEnhancementManager().rerenderItemLore(item, itemId, itemManager.getEnhanceLevel(item), meta);

        // 8. [버그 발생 지점] PDC가 수정된 meta를 저장 (AuraSkills NBT 삭제됨)
        item.setItemMeta(meta);

        // 9. [버그 해결] 삭제된 AuraSkills 기본 스탯 NBT를 '재적용'
        dev.aurelium.auraskills.api.item.ItemManager auraSkillsItemManager = plugin.getAuraSkillsBukkit().getItemManager();
        Map<Stat, Double> baseStats = itemManager.getBaseStats(itemId);

        for (Map.Entry<Stat, Double> entry : baseStats.entrySet()) {
            auraSkillsItemManager.addStatModifier(item, ModifierType.ITEM, entry.getKey(), entry.getValue(), false);
        }

        return true;
    }
}