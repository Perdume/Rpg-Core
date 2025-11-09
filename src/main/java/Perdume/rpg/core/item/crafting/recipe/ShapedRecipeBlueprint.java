package Perdume.rpg.core.item.crafting.recipe;

import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * YML의 `type: SHAPED` (순서가 있는) 조합법을 나타내는 클래스입니다.
 * [최종 수정]
 */
public class ShapedRecipeBlueprint implements CustomRecipe {

    private final ItemStack result;
    // [수정] 슬롯 번호(0-8)와 해당 슬롯에 필요한 재료 명세서
    private final Map<Integer, IngredientBlueprint> slotIngredients;

    public ShapedRecipeBlueprint(ItemStack result, Map<Integer, IngredientBlueprint> slotIngredients) {
        this.result = result;
        this.slotIngredients = slotIngredients;
    }

    public Map<Integer, IngredientBlueprint> getSlotIngredients() {
        return slotIngredients;
    }

    @Override
    public boolean matches(ItemStack[] matrix) {
        // matrix는 항상 9칸(0-8)이라고 가정 (CustomCraftGUIListener가 9칸으로 만들어 줌)

        for (int i = 0; i < 9; i++) {
            ItemStack itemInSlot = (matrix.length > i) ? matrix[i] : null;
            IngredientBlueprint requiredIngredient = slotIngredients.get(i); // 0-8 키

            if (requiredIngredient != null) {
                // 이 슬롯(i)에 재료가 정의되어 있음
                if (!requiredIngredient.matches(itemInSlot)) {
                    // 필요한 재료와 실제 아이템이 일치하지 않음
                    return false;
                }
            } else {
                // 이 슬롯(i)은 YML에 정의되지 않음 (즉, 비어있어야 함)
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