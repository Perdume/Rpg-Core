package Perdume.rpg.core.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import Perdume.rpg.core.item.ItemManager; // ItemManager import 확인

public class CustomItemUtil {

    public static String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        // ItemManager.CUSTOM_ITEM_ID_KEY가 static이므로 정상 접근 가능
        if (container.has(ItemManager.CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING)) {
            return container.get(ItemManager.CUSTOM_ITEM_ID_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    public static boolean isCustomItem(ItemStack item) {
        return getItemId(item) != null;
    }

    /**
     * 두 아이템 스택을 비교합니다. (커스텀/바닐라 모두 호환)
     */
    public static boolean isSimilar(ItemStack item1, ItemStack item2) {
        if (item1 == null && item2 == null) {
            return true;
        }
        if (item1 == null || item2 == null) {
            return false;
        }

        String id1 = getItemId(item1);
        String id2 = getItemId(item2);

        if (id1 != null && id2 != null) {
            // 둘 다 커스텀 아이템인 경우 ID만 비교
            return id1.equals(id2);
        } else if (id1 == null && id2 == null) {
            // 둘 다 커스텀 아이템이 아닌 경우 (바닐라 아이템)
            return item1.isSimilar(item2);
        } else {
            // 하나는 커스텀, 하나는 바닐라 아이템인 경우
            return false;
        }
    }
}