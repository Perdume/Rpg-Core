package Perdume.rpg.core.item.crafting.recipe;

import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

/**
 * 모든 YML 기반 커스텀 조합법이 구현해야 하는 인터페이스입니다.
 */
public interface CustomRecipe {

    /**
     * 현재 조합창의 상태가 이 레시피와 일치하는지 확인합니다.
     * @param inventory 조합창 인벤토리
     * @return 일치하면 true
     */
    boolean matches(CraftingInventory inventory);

    /**
     * 이 레시피의 결과 아이템을 반환합니다.
     * @return 조합 결과물
     */
    ItemStack getResult();
}

