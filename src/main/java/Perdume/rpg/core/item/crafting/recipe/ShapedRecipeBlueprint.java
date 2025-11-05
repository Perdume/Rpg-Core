package Perdume.rpg.core.item.crafting.recipe;

import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * YML의 `type: SHAPED` (순서가 있는) 조합법을 나타내는 클래스입니다.
 */
public class ShapedRecipeBlueprint implements CustomRecipe {

    private final ItemStack result;
    // 슬롯 번호(1-9)와 해당 슬롯에 필요한 재료 명세서
    private final Map<Integer, IngredientBlueprint> slotIngredients;

    public ShapedRecipeBlueprint(ItemStack result, Map<Integer, IngredientBlueprint> slotIngredients) {
        this.result = result;
        this.slotIngredients = slotIngredients;
    }

    @Override
    public boolean matches(CraftingInventory inventory) {
        ItemStack[] matrix = inventory.getMatrix(); // 0~8 인덱스의 9칸 배열

        for (int i = 0; i < 9; i++) {
            ItemStack itemInSlot = matrix[i];
            IngredientBlueprint requiredIngredient = slotIngredients.get(i + 1); // YML은 1-9, 배열은 0-8

            if (requiredIngredient != null) {
                // 이 슬롯(i+1)에 재료가 정의되어 있음
                if (!requiredIngredient.matches(itemInSlot)) {
                    // 필요한 재료와 실제 아이템이 일치하지 않음
                    return false;
                }
            } else {
                // 이 슬롯(i+1)은 YML에 정의되지 않음 (즉, 비어있어야 함)
                if (itemInSlot != null && !itemInSlot.getType().isAir()) {
                    // 비어있어야 할 슬롯에 아이템이 있음
                    return false;
                }
            }
        }
        // 모든 슬롯 검사를 통과
        return true;
    }

    @Override
    public ItemStack getResult() {
        return this.result.clone();
    }
}
