package Perdume.rpg.core.item;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 플러그인에 존재하는 모든 커스텀 아이템을 등록하고 관리하는 중앙 등록소입니다.
 */
public class ItemManager {

    // 등록 순서를 보장하기 위해 LinkedHashMap을 사용합니다.
    private final Map<String, ItemStack> customItems = new LinkedHashMap<>();

    public ItemManager() {
        registerItems();
    }

    /**
     * 이곳에 플러그인의 모든 커스텀 아이템을 등록합니다.
     * 새로운 커스텀 아이템 Enum을 만들어도 이곳에 한 줄만 추가하면 됩니다.
     */
    private void registerItems() {
        // 뒤틀린 광물 (SpecialOre) 등록
        for (twisted_item ore : twisted_item.values()) {
            registerItem("special_ore_" + ore.name().toLowerCase(), ore.getItemStack(1));
        }

        // 뒤틀린 조약돌 (TwistedCobblestone) 등록
        for (TwistedCobblestone stone : TwistedCobblestone.values()) {
            registerItem("twisted_cobblestone_" + stone.name().toLowerCase(), stone.getItemStack(1));
        }

        // TODO: 나중에 새로운 아이템 그룹(예: 강화석, 포션 등)을 추가할 때 여기에 등록 로직을 추가합니다.
    }

    /**
     * 커스텀 아이템을 등록소에 추가합니다.
     * @param key 아이템을 식별할 고유 키
     * @param itemStack 등록할 아이템
     */
    public void registerItem(String key, ItemStack itemStack) {
        customItems.put(key, itemStack);
    }

    /**
     * 등록된 모든 커스텀 아이템의 리스트를 반환합니다.
     * @return 아이템 목록
     */
    public List<ItemStack> getAllItems() {
        return new ArrayList<>(customItems.values());
    }
}
