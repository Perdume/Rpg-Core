package Perdume.rpg.core.item.crafting.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.CustomItemUtil;
import Perdume.rpg.core.item.crafting.RecipeManager;
import Perdume.rpg.core.item.crafting.recipe.CustomRecipe;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 바닐라 조합창(인벤토리 2x2 포함)의 이벤트를 감지하여
 * 커스텀 레시피가 작동하도록 처리하는 리스너입니다.
 */
public class CustomCraftingListener implements Listener {

    private final RecipeManager recipeManager;

    public CustomCraftingListener(Rpg plugin) {
        this.recipeManager = plugin.getRecipeManager();
    }

    /**
     * 조합창의 아이템이 변경될 때마다(결과물이 표시되기 직전) 호출됩니다.
     * (Priority = HIGHEST : 바닐라 조합법을 덮어쓰기 위해 우선순위를 높게 설정)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        if (inventory.getMatrix() == null) {
            return;
        }

        ItemStack[] matrix = inventory.getMatrix(); // [추가됨]

        CustomRecipe matchedRecipe = null;
        // RecipeManager에 등록된 모든 커스텀 레시피와 대조
        for (Map.Entry<String, CustomRecipe> entry : recipeManager.getRecipes().entrySet()) {
            CustomRecipe recipe = entry.getValue();

            if (recipe.matches(matrix)) { // [수정됨]
                matchedRecipe = recipe;
                break;
            }
        }

        if (matchedRecipe != null) {
            // 일치하는 커스텀 레시피가 있으면, 결과 슬롯에 아이템을 강제로 설정
            inventory.setResult(matchedRecipe.getResult());
        } else {
            // 일치하는 커스텀 레시피가 없음.
            // (혹시 이전에 설정된 커스텀 아이템이 남아있다면) 결과 슬롯을 비웁니다.
            ItemStack currentResult = inventory.getResult();
            if (currentResult != null && CustomItemUtil.getItemId(currentResult) != null) {
                inventory.setResult(null);
            }
            // (바닐라 조합법은 그대로 작동하도록 둡니다)
        }
    }
}