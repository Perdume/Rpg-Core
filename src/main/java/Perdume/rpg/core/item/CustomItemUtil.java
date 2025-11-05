package Perdume.rpg.core.item;

import Perdume.rpg.Rpg;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 커스텀 아이템의 고유 ID (NBT 태그)를 읽고 쓰는 유틸리티 클래스입니다.
 */
public class CustomItemUtil {

    // 모든 커스텀 아이템이 공통으로 가질 NBT 키
    public static final NamespacedKey CUSTOM_ID_KEY = new NamespacedKey(Rpg.getInstance(), "rpg_item_id");

    /**
     * 아이템에 커스텀 ID를 설정합니다.
     * @param itemStack ID를 설정할 아이템
     * @param customId 설정할 고유 ID (예: "twisted_cobblestone_t1")
     * @return ID가 설정된 아이템
     */
    public static ItemStack setCustomId(ItemStack itemStack, String customId) {
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack; // 비정상적인 아이템(메타데이터 없음)

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(CUSTOM_ID_KEY, PersistentDataType.STRING, customId);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * 아이템에서 커스텀 ID를 읽어옵니다.
     * @param itemStack ID를 읽을 아이템
     * @return 아이템의 고유 ID, 없으면 null
     */
    public static String getCustomId(ItemStack itemStack) {
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(CUSTOM_ID_KEY, PersistentDataType.STRING)) {
            return container.get(CUSTOM_ID_KEY, PersistentDataType.STRING);
        }
        return null;
    }
}
