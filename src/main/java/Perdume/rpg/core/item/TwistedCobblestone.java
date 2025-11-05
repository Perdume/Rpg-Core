package Perdume.rpg.core.item;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 티어별 '뒤틀린 조약돌' 아이템의 모든 정보를 정의하고 관리하는 Enum 클래스입니다.
 */
public enum TwistedCobblestone {

    TIER_1(1, "§f뒤틀린 조약돌 I", Material.COBBLESTONE, 2001, "§7가장 기본적인 형태로 압축된 조약돌입니다."),
    TIER_2(2, "§a뒤틀린 조약돌 II", Material.MOSSY_COBBLESTONE, 2002, "§7시간의 흔적이 느껴지는 조약돌입니다."),
    TIER_3(3, "§9뒤틀린 조약돌 III", Material.DEEPSLATE, 2003, "§7심연의 압력을 견뎌낸 조약돌입니다."),
    TIER_4(4, "§5뒤틀린 조약돌 IV", Material.BLACKSTONE, 2004, "§7불길한 기운이 서려있는 조약돌입니다."),
    TIER_5(5, "§6뒤틀린 조약돌 Ⅴ", Material.GILDED_BLACKSTONE, 2005, "§7마침내 정수가 드러난 궁극의 조약돌입니다.");

    private final int tier;
    private final String displayName;
    private final Material material;
    private final int customModelData;
    private final String[] lore;
    private final NamespacedKey itemKey; // 아이템을 구분하기 위한 고유 키

    TwistedCobblestone(int tier, String displayName, Material material, int customModelData, String... lore) {
        this.tier = tier;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.lore = lore;
        this.itemKey = new NamespacedKey(Rpg.getInstance(), "twisted_cobblestone_tier");
    }

    /**
     * 이 Enum에 정의된 정보를 바탕으로 실제 ItemStack 객체를 생성하여 반환합니다.
     * @param amount 생성할 아이템의 개수
     * @return 완성된 ItemStack
     */
    public ItemStack getItemStack(int amount) {
        ItemStack item = new ItemFactory(this.material)
                .setDisplayName(this.displayName)
                .setLore(this.lore)
                .setCustomModelData(this.customModelData)
                .build(amount);
        
        // 아이템 메타데이터에 티어 정보를 영구적으로 저장합니다.
        item.editMeta(meta -> meta.getPersistentDataContainer().set(itemKey, PersistentDataType.INTEGER, this.tier));
        return item;
    }

    /**
     * 주어진 ItemStack이 어떤 티어의 뒤틀린 조약돌인지 확인합니다.
     * @param item 확인할 ItemStack
     * @return 티어 번호 (1-5), 해당 아이템이 아니면 0
     */
    public static int getTierFromItemStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        NamespacedKey key = new NamespacedKey(Rpg.getInstance(), "twisted_cobblestone_tier");
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return tier != null ? tier : 0;
    }

    /**
     * 티어 번호로 해당하는 TwistedCobblestone Enum 상수를 찾습니다.
     * @param tier 찾을 티어 번호
     * @return 해당하는 Enum 상수, 없으면 null
     */
    public static TwistedCobblestone getByTier(int tier) {
        for (TwistedCobblestone value : values()) {
            if (value.tier == tier) {
                return value;
            }
        }
        return null;
    }
}

