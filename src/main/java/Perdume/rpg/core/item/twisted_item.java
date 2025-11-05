package Perdume.rpg.core.item;

import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 섬에서 채광 시 특별한 확률로 드롭되는 '뒤틀린 광석'의 모든 정보를 정의하고 관리하는 Enum 클래스입니다.
 */
public enum twisted_item {

    // --- Enum 상수 정의 ---
    // 이름, Lore, 빛나는 효과만 추가하고, 아이템 종류는 원래 광물의 드롭템을 사용합니다.
    DARK_FRAGMENT(
            EnumSet.of(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE), 0.1, "§8어둠에 물든 석탄",
            Material.COAL, 1000, true,
            "§7평범한 석탄과 달리, 불길한 기운이 느껴집니다."
    ),
    TWISTED_STEEL(
            EnumSet.of(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE), 0.2, "§b뒤틀린 철 원석",
            Material.RAW_IRON, 1001, true,
            "§7알 수 없는 힘에 의해 뒤틀린 철 원석입니다."
    ),
    GOLDEN_CORE(
            EnumSet.of(Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE), 0.3, "§6왜곡된 금 원석",
            Material.RAW_GOLD, 1002, true,
            "§7황금 이상의 가치를 지닌 듯한 왜곡의 핵입니다."
    ),
    MANA_CRYSTAL(
            EnumSet.of(Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE), 0.3, "§9마력이 깃든 청금석",
            Material.LAPIS_LAZULI, 1003, true,
            "§7순수한 마력의 결정체로, 강력한 힘을 발산합니다."
    ),
    CHAOS_CIRCUIT(
            EnumSet.of(Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE), 0.4, "§c혼돈의 레드스톤",
            Material.REDSTONE, 1004, true,
            "§7일반적인 회로와는 다른, 예측 불가능한 에너지를 품고 있습니다."
    ),
    ABYSS_CRYSTAL(
            EnumSet.of(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE), 0.5, "§d심연의 다이아몬드",
            Material.DIAMOND, 1005, true,
            "§7세상의 모든 빛을 빨아들일 듯한 불길한 결정입니다."
    );

    // --- Enum 필드 ---
    private final Set<Material> sourceOres;
    private final double dropChance;
    private final String displayName;
    private final Material baseItemMaterial; // 아이템의 기본 재료 (예: RAW_IRON, COAL)
    private final int customModelData;
    private final boolean glowing; // 빛나는 효과 여부
    private final String[] lore;

    /**
     * SpecialOre Enum 생성자
     */
    twisted_item(Set<Material> sourceOres, double dropChance, String displayName, Material baseItemMaterial, int customModelData, boolean glowing, String... lore) {
        this.sourceOres = sourceOres;
        this.dropChance = dropChance;
        this.displayName = displayName;
        this.baseItemMaterial = baseItemMaterial;
        this.customModelData = customModelData;
        this.glowing = glowing;
        this.lore = lore;
    }

    // --- 공개 메소드 ---
    public double getDropChance() {
        return dropChance;
    }

    /**
     * 이 Enum에 정의된 정보를 바탕으로 실제 ItemStack 객체를 생성하여 반환합니다.
     * @param amount 생성할 아이템의 개수
     * @return 완성된 ItemStack
     */
    public ItemStack getItemStack(int amount) {
        ItemFactory factory = new ItemFactory(this.baseItemMaterial)
                .setDisplayName(this.displayName)
                .setLore(this.lore)
                .setCustomModelData(this.customModelData);

        // glowing 플래그가 true이면, 빛나는 효과를 적용합니다.
        if (this.glowing) {
            factory.addEnchant(Enchantment.LURE, 1, true); // 아무 인챈트나 적용
            factory.addItemFlags(ItemFlag.HIDE_ENCHANTS);      // 인챈트 문구 숨기기
        }

        return factory.build(amount);
    }

    // --- Static 영역 (효율적인 조회를 위함) ---
    private static final Map<Material, twisted_item> ORE_MAP = new HashMap<>();

    // 서버 로드 시, 모든 광물 정보를 미리 맵에 저장하여 검색 속도를 높입니다.
    static {
        for (twisted_item specialOre : values()) {
            for (Material sourceOre : specialOre.sourceOres) {
                ORE_MAP.put(sourceOre, specialOre);
            }
        }
    }

    /**
     * 부서진 광물(Material)에 해당하는 SpecialOre를 맵에서 빠르게 찾습니다.
     * @param material 조회할 광물의 Material
     * @return Optional<SpecialOre> (없을 경우 Optional.empty())
     */
    public static Optional<twisted_item> getBySource(Material material) {
        return Optional.ofNullable(ORE_MAP.get(material));
    }
}