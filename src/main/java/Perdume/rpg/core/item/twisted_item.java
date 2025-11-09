package Perdume.rpg.core.item;

import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Optional;

/**
 * 섬 레벨에 따라 광물 생성기에서 확률적으로 드랍되는 아이템
 */
public enum twisted_item {

    DARK_FRAGMENT(
            EnumSet.of(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE), 0.1, "§8어둠에 물든 석탄",
            Material.COAL, 1000, true, // 'true'가 'something' 필드 (빛나는 효과)
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

    private final EnumSet<Material> sourceOres; // 이 아이템을 드랍하는 원본 광석
    private final double dropChance; // 드랍 확률
    private final String name; // 아이템 이름
    private final Material dropItem; // 아이템의 기반 재료
    private final int customModelData; // 커스텀 모델 데이터
    private final boolean something; // 'glowing' 대신 'something' 필드
    private final String lore; // 아이템 설명

    twisted_item(EnumSet<Material> sourceOres, double dropChance, String name, Material dropItem, int customModelData, boolean something, String lore) {
        this.sourceOres = sourceOres;
        this.dropChance = dropChance;
        this.name = name;
        this.dropItem = dropItem;
        this.customModelData = customModelData;
        this.something = something;
        this.lore = lore;
    }

    /**
     * ItemManager가 사용할 고유 ID
     * @return "twisted_item_dark_fragment"
     */
    public String getItemId() {
        return "twisted_item_" + this.name().toLowerCase();
    }

    /**
     * [수정됨] 새로운 ItemFactory 기준에 맞게 메서드 호출 변경
     * @param amount 수량
     * @return ItemStack
     */
    public ItemStack getItemStack(int amount) {
        // ItemFactory를 사용하여 NBT 태그를 포함한 아이템 생성
        ItemFactory factory = new ItemFactory(this.dropItem)
                // .setAmount(amount) // build(amount)가 처리
                .setDisplayName(this.name)      // .setName -> .setDisplayName
                .setLore(this.lore)             // .addLore -> .setLore (String...은 단일 String도 받음)
                .setCustomModelData(this.customModelData)
                .setCustomItemId(this.getItemId()); // NBT 태그 추가

        // 'something' (기존 'glowing') 플래그가 true이면, 빛나는 효과 적용
        if (this.something) {
            factory.addEnchant(Enchantment.LURE, 1, true); // 아무 인챈트나 적용
            factory.addItemFlags(ItemFlag.HIDE_ENCHANTS);    // 인챈트 문구 숨기기
        }

        return factory.build(amount); // .create() -> .build(amount)
    }

    // Getter (필요시 사용)
    public EnumSet<Material> getSourceOres() {
        return sourceOres;
    }

    public double getDropChance() {
        return dropChance;
    }

    public String getName() {
        return name;
    }

    /**
     * 원본 광물(Material)을 기반으로 일치하는 twisted_item을 찾습니다.
     * (CobblestoneGeneratorListener 등에서 사용)
     *
     * @param material 플레이어가 캔 광물 (예: Material.COAL_ORE)
     * @return 일치하는 twisted_item (없으면 null)
     */
    public static Optional<twisted_item> getBySource(Material material) {
        for (twisted_item item : values()) {
            if (item.getSourceOres().contains(material)) {
                return Optional.of(item);
            }
        }
        return null; // 일치하는 아이템 없음
    }
}