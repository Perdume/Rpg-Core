package Perdume.rpg.core.item.crafting.gui;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.crafting.RecipeManager;
import Perdume.rpg.core.item.crafting.recipe.CustomRecipe;
import Perdume.rpg.core.item.crafting.recipe.IngredientBlueprint;
import Perdume.rpg.core.item.crafting.recipe.ShapedRecipeBlueprint;
import Perdume.rpg.core.item.crafting.recipe.ShapelessRecipeBlueprint;
import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 특정 레시피의 3x3 조합법을 보여주는 GUI
 */
public class RecipeViewerGUI implements InventoryHolder {

    private final Inventory gui;
    private final Player player;
    private final RecipeManager recipeManager;
    private final int previousPage; // 뒤로가기 시 돌아갈 페이지
    private final Rpg plugin; // Rpg 플러그인 인스턴스

    // GUI 슬롯 정의 (CustomCraftGUI와 동일)
    private final List<Integer> craftingSlots = List.of(
            10, 11, 12, 19, 20, 21, 28, 29, 30
    );
    private final int RESULT_SLOT = 24;
    private final int BACK_BUTTON_SLOT = 45; // 뒤로가기 버튼
    private final ItemStack BORDER_ITEM = new ItemFactory(Material.GRAY_STAINED_GLASS_PANE)
            .setDisplayName("§r")
            .build();
    private final ItemStack BACK_BUTTON_ITEM = new ItemFactory(Material.BARRIER)
            .setDisplayName("§c뒤로가기")
            .addLore("§7레시피 목록으로 돌아갑니다.")
            .build();

    public RecipeViewerGUI(Rpg plugin, RecipeManager recipeManager, Player player, CustomRecipe recipe, int previousPage) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.player = player;
        this.previousPage = previousPage;
        this.gui = Bukkit.createInventory(this, 54, "조합법 상세보기");
        initializeItems(recipe);
    }

    private void initializeItems(CustomRecipe recipe) {
        // 1. 테두리 채우기
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, BORDER_ITEM);
        }

        // 2. 뒤로가기 버튼
        gui.setItem(BACK_BUTTON_SLOT, BACK_BUTTON_ITEM);

        // 3. 결과물 배치
        gui.setItem(RESULT_SLOT, recipe.getResult());

        // 4. 재료 배치
        if (recipe instanceof ShapedRecipeBlueprint) {
            // Shaped: 0-8 인덱스를 10, 11... 슬롯으로 변환
            Map<Integer, IngredientBlueprint> slotIngredients = ((ShapedRecipeBlueprint) recipe).getSlotIngredients();
            for (Map.Entry<Integer, IngredientBlueprint> entry : slotIngredients.entrySet()) {
                int matrixIndex = entry.getKey(); // 0-8
                int guiSlot = craftingSlots.get(matrixIndex); // 10, 11...
                gui.setItem(guiSlot, entry.getValue().getItemStack()); // 수량이 포함된 ItemStack
            }
        } else if (recipe instanceof ShapelessRecipeBlueprint) {
            // Shapeless: 0-8 슬롯에 순서대로 채우기
            List<IngredientBlueprint> ingredients = ((ShapelessRecipeBlueprint) recipe).getRequiredIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                if (i >= 9) break; // 9칸 초과 재료는 표시하지 않음
                int guiSlot = craftingSlots.get(i);
                gui.setItem(guiSlot, ingredients.get(i).getItemStack());
            }
        }
    }

    public void open() {
        player.openInventory(gui);
    }

    public int getPreviousPage() {
        return previousPage;
    }

    public Rpg getPlugin() {
        return plugin;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public Inventory getInventory() {
        return this.gui;
    }
}