package Perdume.rpg.core.item;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.stats.RpgCustomStats;
import Perdume.rpg.core.util.ItemFactory;
import dev.aurelium.auraskills.api.AuraSkillsApi;
// [AI] 2.x API: AuraSkillsBukkit import
import dev.aurelium.auraskills.api.AuraSkillsBukkit;
// [AI] 2.2.4 API import
import dev.aurelium.auraskills.api.item.ModifierType;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills; // [AI] 2.2.4 API
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.stat.StatModifier; // [AI] 2.2.4 API
import dev.aurelium.auraskills.api.stat.Stats;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * [YML 기반으로 수정됨 - 2.x API 및 1.21+ Bukkit API 호환]
 * [하이브리드 로드]
 * 1. Enum (TwistedCobblestone, twisted_item)에서 '재료' 아이템을 로드
 * 2. YML (items.yml)에서 '장비' 아이템을 로드
 */
public class ItemManager {

    // --- NBT 키 정의 (플러그인 전체 공유) ---
    public static NamespacedKey CUSTOM_ITEM_ID_KEY;
    public static NamespacedKey CUSTOM_ITEM_VERSION_KEY;
    public static NamespacedKey ITEM_TYPE_KEY;
    public static NamespacedKey ENHANCE_LEVEL_KEY;
    public static NamespacedKey ENHANCE_STATS_KEY;
    public static NamespacedKey POTENTIAL_GRADE_KEY;
    public static NamespacedKey POTENTIAL_STATS_KEY;
    // [AI] 스탯 요구사항을 수동 저장하기 위한 NBT 키
    public static NamespacedKey CUSTOM_REQUIREMENTS_KEY;
    // [버그 수정] 기본 스탯을 저장할 고유 NBT 키
    public static NamespacedKey CUSTOM_BASE_STATS_KEY;


    private final Rpg plugin;
    private final AuraSkillsApi auraSkillsApi;
    // [AI] 2.x API는 ItemManager가 Bukkit API에 포함됩니다.
    private final dev.aurelium.auraskills.api.item.ItemManager auraSkillsItemManager;

    private final Map<String, ItemStack> customItemRegistry = new LinkedHashMap<>();

    private File itemsFile;
    private File versionsFile;
    private FileConfiguration itemsConfig;
    private FileConfiguration versionsConfig;
    private final Map<String, Integer> itemMasterVersions = new HashMap<>();

    // [AI] 2.x API: AuraSkillsBukkit 인스턴스 주입
    public ItemManager(Rpg plugin, AuraSkillsApi auraSkillsApi, AuraSkillsBukkit auraSkillsBukkit) {
        this.plugin = plugin;
        this.auraSkillsApi = auraSkillsApi;
        // [AI] 2.x API에 맞게 AuraSkillsBukkit 인스턴스 가져오기
        this.auraSkillsItemManager = auraSkillsBukkit.getItemManager();

        CUSTOM_ITEM_ID_KEY = new NamespacedKey(plugin, "custom-item-id");
        CUSTOM_ITEM_VERSION_KEY = new NamespacedKey(plugin, "custom-item-version");
        ITEM_TYPE_KEY = new NamespacedKey(plugin, "custom-item-type");
        ENHANCE_LEVEL_KEY = new NamespacedKey(plugin, "custom-item-enhance-level");
        ENHANCE_STATS_KEY = new NamespacedKey(plugin, "custom-item-enhance-stats");
        POTENTIAL_GRADE_KEY = new NamespacedKey(plugin, "custom-potential-grade");
        POTENTIAL_STATS_KEY = new NamespacedKey(plugin, "custom-potential-stats");
        // [AI] 스탯 요구사항 NBT 키 초기화
        CUSTOM_REQUIREMENTS_KEY = new NamespacedKey(plugin, "custom-item-requirements");
        CUSTOM_BASE_STATS_KEY = new NamespacedKey(plugin, "custom-base-stats");

        this.itemsFile = new File(plugin.getDataFolder(), "items.yml");
        this.versionsFile = new File(plugin.getDataFolder(), "item_versions.yml");
    }

    /**
     * Rpg.java의 onEnable에서 호출됩니다.
     */
    public void loadItems() {
        customItemRegistry.clear();
        itemMasterVersions.clear();

        // --- 1. Enum 재료템 등록 ---
        registerMaterialEnums();

        // --- 2. YML 장비템 등록 ---
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }
        if (!versionsFile.exists()) {
            plugin.saveResource("item_versions.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        versionsConfig = YamlConfiguration.loadConfiguration(versionsFile);

        ConfigurationSection versionsSection = versionsConfig.getConfigurationSection("versions");
        if (versionsSection != null) {
            versionsSection.getKeys(false).forEach(key -> itemMasterVersions.put(key, versionsSection.getInt(key)));
        }
        Rpg.log.info("Loaded " + itemMasterVersions.size() + " item versions from YML.");

        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            Rpg.log.info("'items.yml'에 'items:' 섹션이 없습니다. (장비템 0개)");
            return;
        }

        int equipmentCount = 0;
        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) continue;

            try {
                // [AI] 2.x API에 맞게 ItemFactory 및 수동 Lore/NBT 적용
                Material material = Material.matchMaterial(itemSection.getString("base-material", "STONE"));
                ItemFactory factory = new ItemFactory(material)
                        .setDisplayName(itemSection.getString("display-name", "§f" + itemId))
                        .setCustomModelData(itemSection.getInt("custom-model-data", 0));

                List<String> baseLore = itemSection.getStringList("lore");
                // [AI] 2.x API에서는 Lore를 수동으로 빌드
                List<String> statsLore = buildAuraSkillsLore(itemSection);

                if (!baseLore.isEmpty()) factory.addLore(baseLore.toArray(new String[0]));
                if (!statsLore.isEmpty()) {
                    factory.addLore("§7--------------------");
                    factory.addLore(statsLore.toArray(new String[0]));
                }

                if (itemSection.getBoolean("glowing", false)) {
                    factory.addEnchant(Enchantment.LURE, 1, true);
                    factory.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                ItemStack finalTemplateItem = factory.build();

                // [AI] 2.x API: AuraSkills NBT를 *ItemStack*에 직접 적용
                // (이 메소드가 내부적으로 ItemMeta를 가져와서 수정하고 다시 set함)
                applyNbtData(finalTemplateItem, itemSection, itemId);


                // '마스터 템플릿'을 레지스트리에 등록
                customItemRegistry.put(itemId, finalTemplateItem);
                equipmentCount++;

            } catch (Exception e) {
                Rpg.log.severe("장비 아이템 '" + itemId + "' 로드 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }
        Rpg.log.info("Successfully loaded " + equipmentCount + " equipment items from items.yml.");
        Rpg.log.info("Total " + customItemRegistry.size() + " custom items registered.");
    }

    /**
     * [AI] 2.x API: NBT(PDC)에 스탯과 요구사항을 *직접* 저장합니다.
     * [최종 버그 수정] NBT 덮어쓰기 문제를 해결하기 위해,
     * 'AuraSkills NBT'를 먼저 저장하고, '우리 커스텀 NBT'를 그 위에 덮어씌워 저장합니다.
     * [사용자 요청] AuraSkills NBT와 별개로, '고유 PDC'에 기본 스탯을 이중 저장합니다.
     */
    private void applyNbtData(ItemStack item, ConfigurationSection itemSection, String itemId) {
        // [AI] 2.2.4 API: ModifierType.ITEM을 사용 (항상 적용되는 기본값)
        ModifierType type = ModifierType.ITEM;

        // --- 1. 'AuraSkills API' NBT 적용 (스탯) ---
        // (AuraSkills API는 내부적으로 setItemMeta를 호출하므로 '커스텀 NBT' 저장 전에 실행)
        ConfigurationSection statsSection = itemSection.getConfigurationSection("stats");
        if (statsSection != null) {
            for (String statId : statsSection.getKeys(false)) {
                Stat stat = getStatFromString(statId);
                if (stat != null) {
                    double value = statsSection.getDouble(statId);
                    auraSkillsItemManager.addStatModifier(item, type, stat, value, false);
                }
            }
        }

        // --- 2. ItemMeta를 'AuraSkills 스탯이 저장된 후'에 다시 가져옵니다. ---
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Rpg.log.severe(itemId + " 아이템의 ItemMeta가 null이어서 고유 NBT를 적용할 수 없습니다.");
            return; // 치명적 오류
        }
        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        // --- 3. 플러그인 고유 NBT 적용 (커스텀 NBT) ---
        nbt.set(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING, itemId);
        int version = itemMasterVersions.getOrDefault(itemId, 1);
        nbt.set(CUSTOM_ITEM_VERSION_KEY, PersistentDataType.INTEGER, version);

        String itemType = itemSection.getString("item-type");
        if (itemType != null && !itemType.isEmpty()) {
            nbt.set(ITEM_TYPE_KEY, PersistentDataType.STRING, itemType); // [핵심] custom-item-type NBT 저장
        }

        // --- 4. 요구 조건 NBT 적용 (커스텀 NBT) ---
        PersistentDataContainer customReqNbt = nbt.getAdapterContext().newPersistentDataContainer();
        boolean hasCustomReqs = false;
        ConfigurationSection reqSection = itemSection.getConfigurationSection("requirements");
        if (reqSection != null) {
            for (String reqId : reqSection.getKeys(false)) {
                // 4-1. STAT 요구조건 (커스텀 NBT)
                if (reqId.equalsIgnoreCase("STAT")) {
                    ConfigurationSection statReqSection = reqSection.getConfigurationSection(reqId);
                    Stat stat = getStatFromString(statReqSection.getString("type").toLowerCase(Locale.ROOT));
                    if (stat != null) {
                        int value = statReqSection.getInt("value");
                        customReqNbt.set(new NamespacedKey(plugin, stat.name().toLowerCase(Locale.ROOT)), PersistentDataType.INTEGER, value);
                        hasCustomReqs = true;
                    }
                }
                // 4-2. LEVEL 요구조건 (커스텀 NBT)
                else if (reqId.equalsIgnoreCase("LEVEL")) {
                    int value = reqSection.getInt(reqId);
                    customReqNbt.set(new NamespacedKey(plugin, "level"), PersistentDataType.INTEGER, value);
                    hasCustomReqs = true;
                }
            }
        }
        if (hasCustomReqs) {
            nbt.set(CUSTOM_REQUIREMENTS_KEY, PersistentDataType.TAG_CONTAINER, customReqNbt);
        }

        // --- 5. [사용자 요청] 기본 스탯 NBT 백업 (커스텀 NBT) ---
        PersistentDataContainer baseStatsNbt = nbt.getAdapterContext().newPersistentDataContainer();
        boolean hasBaseStats = false;
        if (statsSection != null) {
            for (String statId : statsSection.getKeys(false)) {
                Stat stat = getStatFromString(statId);
                if (stat != null) {
                    double value = statsSection.getDouble(statId);
                    baseStatsNbt.set(new NamespacedKey(plugin, stat.name().toLowerCase(Locale.ROOT)), PersistentDataType.DOUBLE, value);
                    hasBaseStats = true;
                }
            }
        }
        if (hasBaseStats) {
            nbt.set(CUSTOM_BASE_STATS_KEY, PersistentDataType.TAG_CONTAINER, baseStatsNbt);
        }

        // --- 6. [중요] 모든 '커스텀' NBT 변경사항을 ItemMeta에 '마지막으로' 덮어씌워 저장합니다. ---
        item.setItemMeta(meta);

        // --- 7. 'AuraSkills API' NBT 적용 (요구조건 - 현재 사용 안함) ---
        // (LEVEL은 우리가 커스텀 NBT로 처리했으므로, 여기서는 다른 AuraSkills 스킬 요구조건만)
        // ...
    }


    /**
     * Enum으로 정의된 '재료' 아이템들을 레지스트리에 등록합니다.
     */
    private void registerMaterialEnums() {
        int materialCount = 0;

        // 1. TwistedCobblestone.java Enum 로드
        for (TwistedCobblestone stone : TwistedCobblestone.values()) {
            customItemRegistry.put(stone.getItemId(), stone.getItemStack(1));
            materialCount++;
        }

        // 2. twisted_item.java Enum 로드
        for (twisted_item ore : twisted_item.values()) {
            customItemRegistry.put(ore.getItemId(), ore.getItemStack(1));
            materialCount++;
        }
        Rpg.log.info("Successfully loaded " + materialCount + " material items from Enums.");
    }

    public List<ItemStack> getAllItems() {
        return new ArrayList<>(customItemRegistry.values());
    }

    /**
     * RecipeManager가 사용할 아이템 조회 메서드
     */
    public ItemStack getItem(String id) {
        // 1. 커스텀 아이템 맵에서 찾기 (Enum + YML 모두 포함)
        ItemStack item = customItemRegistry.get(id);
        if (item != null) {
            return item.clone(); // 템플릿 복사본 반환
        }

        // 2. 바닐라 아이템 시도
        Material material = Material.matchMaterial(id.toUpperCase());
        if (material != null) {
            return new ItemStack(material);
        }

        // 3. 찾기 실패
        Rpg.log.warning("ItemManager: getItem(" + id + ")을(를) 커스텀 맵과 바닐라에서 모두 찾지 못했습니다.");
        return null;
    }

    /**
     * [AI] 오류 수정을 위해 추가된 메소드 (CustomItemUtil 대체)
     * ItemStack의 NBT(PDC)에서 "custom-item-id"를 읽어 반환합니다.
     * @param item 확인할 ItemStack
     * @return 커스텀 아이템 ID (없으면 null)
     */
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING)) {
            return container.get(CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    /**
     * [AI] 2.x API: Lore를 수동으로 생성합니다.
     */
    private List<String> buildAuraSkillsLore(ConfigurationSection itemSection) {
        List<String> loreLines = new ArrayList<>();

        // 1. 스탯 Lore 생성
        ConfigurationSection statsSection = itemSection.getConfigurationSection("stats");
        if (statsSection != null) {
            loreLines.add("§a[능력치]");
            for (String statId : statsSection.getKeys(false)) {
                double value = statsSection.getDouble(statId);
                Stat stat = getStatFromString(statId);
                if (stat != null) {
                    String statName = getStatDisplayName(stat);
                    loreLines.add(String.format("§7- %s: §e+%.1f", statName, value));
                }
            }
        }

        // 2. 요구 조건 Lore 생성
        ConfigurationSection reqSection = itemSection.getConfigurationSection("requirements");
        if (reqSection != null) {
            loreLines.add("§c[착용 조건]");
            for (String reqId : reqSection.getKeys(false)) {
                if (reqId.equalsIgnoreCase("LEVEL")) {
                    int value = reqSection.getInt(reqId);
                    loreLines.add(String.format("§7- %s: §e%d", "레벨", value));

                } else if (reqId.equalsIgnoreCase("STAT")) {
                    ConfigurationSection statReqSection = reqSection.getConfigurationSection(reqId);
                    String statId = statReqSection.getString("type").toLowerCase(Locale.ROOT);
                    int value = statReqSection.getInt("value");
                    Stat stat = getStatFromString(statId);
                    if (stat != null) {
                        String statName = getStatDisplayName(stat);
                        loreLines.add(String.format("§7- %s 요구: §e%d", statName, value));
                    }
                }
            }
        }
        return loreLines;
    }

    /**
     * [AI] 2.x API 호환 Stat ID 변환기
     */
    public Stat getStatFromString(String statId) {
        String id = statId.toLowerCase(Locale.ROOT);

        // 1. 우리 커스텀 스탯인지 확인
        if (id.equals("physical_power")) { return RpgCustomStats.PHYSICAL_POWER; }
        if (id.equals("magic_power")) { return RpgCustomStats.MAGIC_POWER; }

        // 2. AuraSkills 2.x API: Stats Enum에서 이름으로 찾기
        try {
            return Stats.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {}

        Rpg.log.warning("ItemManager: getStatFromString에서 '"+statId+"' 스탯을 찾을 수 없습니다.");
        return null;
    }

    /**
     * [AI] 2.x API 호환 Stat 이름 변환기
     */
    private String getStatDisplayName(Stat stat) {
        // [AI] 2.x API는 getDisplayName()이 없으므로, RpgCustomStats의 값을 사용
        if (stat.equals(RpgCustomStats.PHYSICAL_POWER)) { return "§c물리 공격력"; }
        if (stat.equals(RpgCustomStats.MAGIC_POWER)) { return "§b마법 공격력"; }

        // [AI] 2.x API: 기본 스탯의 이름은 AuraSkills 내부 메시지 파일에 따름 (임시로 Enum 이름 사용)
        if (stat.equals(Stats.STRENGTH)) { return "§4힘"; }
        if (stat.equals(Stats.HEALTH)) { return "§c체력"; }
        if (stat.equals(Stats.WISDOM)) { return "§9지혜"; }
        if (stat.equals(Stats.TOUGHNESS)) { return "§8방어력"; }
        // (나머지 스탯 추가)

        return stat.name(); // (최후의 수단: "STRENGTH")
    }

    /**
     * [AI] 2.x API: AuraSkills NBT에서 기본 스탯을 읽어옵니다.
     * [UUID 제거] EquipmentListener(슬롯 기반)가 이 메소드를 호출합니다.
     * [최종 버그 수정] NBT 유실을 막기 위해, AuraSkills NBT 대신
     * 'CUSTOM_BASE_STATS_KEY'에 백업된 고유 PDC에서 기본 스탯을 읽어옵니다.
     */
    public Map<Stat, Double> getBaseStats(String itemId) {
        Map<Stat, Double> baseStats = new HashMap<>();
        ItemStack template = customItemRegistry.get(itemId);
        if (template == null || !template.hasItemMeta()) return baseStats;

        PersistentDataContainer nbt = template.getItemMeta().getPersistentDataContainer();

        // 1. 'CUSTOM_BASE_STATS_KEY' NBT에서 기본 스탯을 읽어옵니다.
        if (nbt.has(CUSTOM_BASE_STATS_KEY, PersistentDataType.TAG_CONTAINER)) {
            PersistentDataContainer baseStatsNbt = nbt.get(CUSTOM_BASE_STATS_KEY, PersistentDataType.TAG_CONTAINER);
            if (baseStatsNbt != null) {
                for (NamespacedKey key : baseStatsNbt.getKeys()) {
                    Stat stat = getStatFromString(key.getKey());
                    if (stat != null) {
                        baseStats.put(stat, baseStatsNbt.getOrDefault(key, PersistentDataType.DOUBLE, 0.0));
                    }
                }
            }
        }
        // 2. [폴백] 만약 고유 PDC가 없다면(구버전 템플릿), AuraSkills NBT에서 읽기 시도
        else {
            Rpg.log.warning("[Rpg] 템플릿 '" + itemId + "'에 'custom-base-stats' NBT가 없어 AuraSkills NBT로 폴백합니다.");
            List<StatModifier> modifiers = auraSkillsItemManager.getStatModifiers(template, ModifierType.ITEM);
            for (StatModifier modifier : modifiers) {
                baseStats.put(modifier.stat(), modifier.value());
            }
        }

        return baseStats;
    }

    /**
     * [AI] 2.x API: AuraSkills NBT(스킬)와 우리 NBT(스탯)에서 요구 조건을 읽어옵니다.
     */
    public Map<String, Object> getRequirements(String itemId) {
        Map<String, Object> requirements = new HashMap<>();
        ItemStack template = customItemRegistry.get(itemId);
        if (template == null || !template.hasItemMeta()) return requirements;

        ItemMeta meta = template.getItemMeta();
        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        // 1. AuraSkills NBT에서 스킬 요구사항 읽기
        // [AI] 2.2.4 Javadoc: getRequirements(ItemStack, ModifierType)
        Map<Skill, Integer> skillReqs = auraSkillsItemManager.getRequirements(template, ModifierType.ITEM);
        skillReqs.forEach((skill, level) -> {
            if (skill.equals(Skills.AGILITY)) { // (임시)
                requirements.put("LEVEL", level);
            }
        });

        // 2. 우리 고유 NBT에서 스탯 요구사항 읽기
        if (nbt.has(CUSTOM_REQUIREMENTS_KEY, PersistentDataType.TAG_CONTAINER)) {
            PersistentDataContainer customReqNbt = nbt.get(CUSTOM_REQUIREMENTS_KEY, PersistentDataType.TAG_CONTAINER);
            if (customReqNbt != null) {
                for (NamespacedKey key : customReqNbt.getKeys()) {
                    Stat stat = getStatFromString(key.getKey());
                    if (stat != null) {
                        int level = customReqNbt.getOrDefault(key, PersistentDataType.INTEGER, 0);
                        requirements.put("STAT_" + stat.name().toLowerCase(Locale.ROOT), Map.of("stat", stat, "value", level));
                    }
                }
            }
        }

        return requirements;
    }


    public int getMasterVersion(String itemId) {
        return this.itemMasterVersions.getOrDefault(itemId, 1);
    }

    public int getEnhanceLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer nbt = item.getItemMeta().getPersistentDataContainer();
        return nbt.getOrDefault(ENHANCE_LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }

    /**
     * 1.21+ API (adapterContext)를 사용하여 아이템 인스턴스를 생성합니다.
     * [최종 버그 수정] item.setItemMeta() 호출 시 AuraSkills NBT가 유실되는 문제를 해결하기 위해,
     * setItemMeta() 호출 후 AuraSkills 기본 스탯 NBT를 '재적용'합니다.
     */
    public ItemStack createItemInstance(String itemId, int amount) {
        ItemStack template = customItemRegistry.get(itemId);
        if (template == null) {
            Rpg.log.severe("createItemInstance: '"+itemId+"' 템플릿을 찾지 못했습니다.");
            return null;
        }

        ItemStack instanceItem = template.clone();
        instanceItem.setAmount(amount);

        ItemMeta meta = instanceItem.getItemMeta();
        if (meta == null) {
            Rpg.log.severe("createItemInstance: '"+itemId+"' 템플릿의 ItemMeta가 null입니다.");
            return instanceItem;
        }

        PersistentDataContainer nbt = meta.getPersistentDataContainer();

        // 1. 인스턴스 NBT(UUID, 강화레벨 등)를 PDC에 추가
        nbt.set(ENHANCE_LEVEL_KEY, PersistentDataType.INTEGER, 0);

        PersistentDataContainer enhanceStatsNbt = nbt.getAdapterContext().newPersistentDataContainer();
        nbt.set(ENHANCE_STATS_KEY, PersistentDataType.TAG_CONTAINER, enhanceStatsNbt);

        PersistentDataContainer potentialStatsNbt = nbt.getAdapterContext().newPersistentDataContainer();
        nbt.set(POTENTIAL_STATS_KEY, PersistentDataType.TAG_CONTAINER, potentialStatsNbt);
        nbt.set(POTENTIAL_GRADE_KEY, PersistentDataType.STRING, "RARE");

        // (createItemInstance는 'requirements'를 수정하지 않으므로 덮어쓰기 코드는 필요 없음)

        // 2. [버그 발생 지점] PDC가 수정된 meta를 저장
        // 이 호출은 AuraSkills NBT를 삭제합니다.
        instanceItem.setItemMeta(meta);

        // 3. [버그 해결] 삭제된 AuraSkills 기본 스탯 NBT를 템플릿에서 다시 읽어와 '재적용'
        dev.aurelium.auraskills.api.item.ItemManager auraSkillsItemManager = plugin.getAuraSkillsBukkit().getItemManager();
        Map<Stat, Double> baseStats = auraSkillsItemManager.getStatModifiers(template, ModifierType.ITEM)
                .stream()
                .collect(Collectors.toMap(StatModifier::stat, StatModifier::value, Double::sum));

        for (Map.Entry<Stat, Double> entry : baseStats.entrySet()) {
            auraSkillsItemManager.addStatModifier(instanceItem, ModifierType.ITEM, entry.getKey(), entry.getValue(), false);
        }

        return instanceItem;
    }

    public FileConfiguration getItemsConfig() {
        return this.itemsConfig;
    }
}