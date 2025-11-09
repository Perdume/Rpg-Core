package Perdume.rpg.core.item;

import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * 뒤틀린 조약돌 티어 시스템 (최대 5단계)
 * [수정됨] 사용자 요청에 따라 기반 Material 및 설명 변경
 */
public enum TwistedCobblestone {

    // [수정됨] 새로운 Enum 값으로 교체
    TIER_1(1, "§f뒤틀린 조약돌 I", Material.COBBLESTONE, 2001, "§7가장 기본적인 형태로 압축된 조약돌입니다."),
    TIER_2(2, "§a뒤틀린 조약돌 II", Material.MOSSY_COBBLESTONE, 2002, "§7시간의 흔적이 느껴지는 조약돌입니다."),
    TIER_3(3, "§9뒤틀린 조약돌 III", Material.DEEPSLATE, 2003, "§7심연의 압력을 견뎌낸 조약돌입니다."),
    TIER_4(4, "§5뒤틀린 조약돌 IV", Material.BLACKSTONE, 2004, "§7불길한 기운이 서려있는 조약돌입니다."),
    TIER_5(5, "§6뒤틀린 조약돌 Ⅴ", Material.GILDED_BLACKSTONE, 2005, "§7마침내 정수가 드러난 궁극의 조약돌입니다.");

    private final int level;
    private final String name;
    private final String lore;
    private final int customModelData;
    private final Material baseMaterial; // [추가됨] 기반 Material 필드

    /**
     * [수정됨] 새로운 생성자 (Material 파라미터 추가)
     */
    TwistedCobblestone(int level, String name, Material baseMaterial, int customModelData, String lore) {
        this.level = level;
        this.name = name;
        this.baseMaterial = baseMaterial; // [추가됨]
        this.customModelData = customModelData;
        this.lore = lore; // [순서 변경됨]
    }

    /**
     * ItemManager가 사용할 고유 ID
     * @return "twisted_cobblestone_tier_1"
     */
    public String getItemId() {
        return "twisted_cobblestone_tier_" + this.level;
    }

    /**
     * [수정됨] 새로운 ItemFactory 기준 및 this.baseMaterial 사용
     * @param amount 수량
     * @return ItemStack
     */
    public ItemStack getItemStack(int amount) {
        // ItemFactory를 사용하여 NBT 태그를 포함한 아이템 생성
        return new ItemFactory(this.baseMaterial) // [수정됨] Material.COBBLESTONE -> this.baseMaterial
                .setDisplayName(this.name)
                .setLore(this.lore)
                .setCustomModelData(this.customModelData)
                .setCustomItemId(this.getItemId()) // NBT 태그 추가
                .build(amount);
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }
}