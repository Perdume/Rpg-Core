package Perdume.rpg.core.item.crafting.recipe;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * YML의 `type: SHAPELESS` (순서가 없는) 조합법을 나타내는 클래스입니다.
 * [최종 수정]
 */
public class ShapelessRecipeBlueprint implements CustomRecipe {

    private final ItemStack result;
    private final List<IngredientBlueprint> requiredIngredients;

    public ShapelessRecipeBlueprint(ItemStack result, List<IngredientBlueprint> requiredIngredients) {
        this.result = result;
        this.requiredIngredients = requiredIngredients;
    }

    public List<IngredientBlueprint> getRequiredIngredients() {
        return requiredIngredients;
    }

    @Override
    public boolean matches(ItemStack[] matrix) {
        // 조합창에 있는 실제 아이템 목록을 복사
        List<ItemStack> actualItems = new ArrayList<>();
        for (ItemStack item : matrix) { // inventory.getMatrix() -> matrix
            if (item != null && !item.getType().isAir()) {
                actualItems.add(item.clone()); // 복사본 사용
            }
        }

        // 필요한 재료 목록과 실제 아이템 목록의 개수가 다르면 불일치
        if (actualItems.size() != requiredIngredients.size()) {
            return false;
        }

        // 필요한 재료 목록을 복사 (검사하면서 제거하기 위함)
        List<IngredientBlueprint> ingredientsToCheck = new ArrayList<>(requiredIngredients);

        // 실제 아이템을 하나씩 돌면서, 일치하는 재료 명세서를 찾아서 제거
        for (ItemStack item : actualItems) {
            IngredientBlueprint matchedIngredient = null;
            for (IngredientBlueprint required : ingredientsToCheck) {
                if (required.matches(item)) {
                    matchedIngredient = required;
                    break;
                }
            }
            if (matchedIngredient != null) {
                ingredientsToCheck.remove(matchedIngredient);
            } else {
                // 이 아이템과 일치하는 재료 명세서가 없음
                return false;
            }
        }

        // 모든 아이템이 재료 명세서와 일치하여 ingredientsToCheck가 비었다면 성공
        return ingredientsToCheck.isEmpty();
    }

    @Override
    public ItemStack getResult() {
        return this.result.clone();
    }
}