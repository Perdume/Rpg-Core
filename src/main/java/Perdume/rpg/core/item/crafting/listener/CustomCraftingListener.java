package Perdume.rpg.core.item.crafting.listener;

import Perdume.rpg.core.item.crafting.RecipeManager;
import Perdume.rpg.core.item.crafting.recipe.CustomRecipe;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;

/**
 * [조합 엔진 사용] 커스텀 조합 이벤트를 감지하여 RecipeManager에게 처리를 위임합니다.
 */
public class CustomCraftingListener implements Listener {

    private final RecipeManager recipeManager;

    public CustomCraftingListener(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        if (inventory == null) return;

        // RecipeManager에게 현재 조합창과 일치하는 레시피를 찾아달라고 요청
        CustomRecipe matchingRecipe = recipeManager.getMatchingRecipe(inventory);

        if (matchingRecipe != null) {
            // 일치하는 레시피를 찾음 -> 결과물 설정
            event.getInventory().setResult(matchingRecipe.getResult());
        } else {
            // 일치하는 커스텀 레시피 없음 -> 결과물 비움 (바닐라 레시피는 알아서 작동함)
            // event.getInventory().setResult(null); 
            // -> 바닐라 레시피까지 막을 수 있으므로, 커스텀 아이템이 하나라도 있을 때만 비우도록 하는 로직이 필요할 수 있음
            // 지금은 단순하게 둡니다.
        }
    }
}

