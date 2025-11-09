package Perdume.rpg.core.item.crafting.gui;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.CustomItemUtil;
import Perdume.rpg.core.item.ItemManager;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 커스텀 레시피북 GUI (페이지 기능 포함)
 */
public class RecipeBookGUI implements InventoryHolder {

    private final Rpg plugin;
    private final RecipeManager recipeManager;
    private final Player player;
    private final Inventory gui;
    private final List<Map.Entry<String, CustomRecipe>> allRecipes;
    private final int page;
    private int maxPage;

    private final int ITEMS_PER_PAGE = 45;

    // [오류 수정됨] 올바른 생성자
    public RecipeBookGUI(Rpg plugin, RecipeManager recipeManager, Player player) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, "커스텀 레시피북");
        this.allRecipes = new ArrayList<>(recipeManager.getRecipes().entrySet());
        this.page = 1; // open()에서 실제 페이지 설정
        this.maxPage = (int) Math.ceil((double) allRecipes.size() / ITEMS_PER_PAGE);
        if (this.maxPage == 0) this.maxPage = 1; // 레시피가 0개여도 1페이지로 표시
    }

    // [오류 수정됨] open() 시그니처 변경
    public void open(int page) {
        gui.clear();
        int maxPage = (int) Math.ceil((double) allRecipes.size() / ITEMS_PER_PAGE);
        int currentPage = Math.max(1, Math.min(page, maxPage));

        // 플레이어의 인벤토리 재료 현황 (ID, 수량)
        Map<String, Integer> playerIngredients = countPlayerIngredients();

        // 1. 레시피 아이템 배치 (0-44)
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allRecipes.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, CustomRecipe> entry = allRecipes.get(i);
            String recipeId = entry.getKey();
            CustomRecipe recipe = entry.getValue();

            ItemStack resultItem = recipe.getResult().clone();
            boolean canCraft = canPlayerCraft(recipe, playerIngredients);

            // [오류 수정됨] ItemFactory(ItemStack) 생성자 사용
            ItemStack displayItem = new ItemFactory(resultItem)
                    .addLore(
                            "§8--------------------",
                            canCraft ? "§a[제작 가능]" : "§c[재료 부족]",
                            "§7클릭하여 조합법을 확인합니다.",
                            "§8Recipe ID: " + recipeId // 리스너가 ID를 읽을 수 있도록 숨김
                    )
                    .build();

            gui.setItem(i - startIndex, displayItem);
        }

        // 2. 네비게이션 아이템 배치
        // (뒤로가기 버튼 - CustomCraftGUI로)
        gui.setItem(45, new ItemFactory(Material.BARRIER)
                .setDisplayName("§c뒤로가기")
                .addLore("§7제작대로 돌아갑니다.")
                .build());

        // 이전 페이지 버튼
        if (currentPage > 1) {
            gui.setItem(48, new ItemFactory(Material.ARROW)
                    .setDisplayName("§a이전 페이지")
                    .addLore("§7" + (currentPage - 1) + " 페이지로 이동")
                    .build());
        }

        // 페이지 정보
        gui.setItem(49, new ItemFactory(Material.PAPER)
                .setDisplayName("§f페이지 " + currentPage + " / " + maxPage)
                .build());

        // 다음 페이지 버튼
        if (currentPage < maxPage) {
            gui.setItem(50, new ItemFactory(Material.ARROW)
                    .setDisplayName("§a다음 페이지")
                    .addLore("§7" + (currentPage + 1) + " 페이지로 이동")
                    .build());
        }

        player.openInventory(gui);
    }

    /**
     * 플레이어 인벤토리의 모든 아이템을 스캔하여 (ID, 수량) 맵을 만듭니다.
     */
    private Map<String, Integer> countPlayerIngredients() {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            // [오류 수정됨] CustomItemUtil.getItemId 사용
            String itemId = CustomItemUtil.getItemId(item);
            if (itemId != null) {
                // 커스텀 아이템
                counts.put(itemId, counts.getOrDefault(itemId, 0) + item.getAmount());
            } else {
                // 바닐라 아이템 (Material 이름 사용)
                String materialName = item.getType().name();
                counts.put(materialName, counts.getOrDefault(materialName, 0) + item.getAmount());
            }
        }
        return counts;
    }

    /**
     * 플레이어가 이 레시피를 제작할 재료가 있는지 확인합니다.
     */
    private boolean canPlayerCraft(CustomRecipe recipe, Map<String, Integer> playerIngredients) {
        // 재료 목록을 ID별로 합산
        Map<String, Integer> requiredCounts = new HashMap<>();
        
        List<IngredientBlueprint> ingredients = new ArrayList<>();
        if (recipe instanceof ShapedRecipeBlueprint) {
            ingredients.addAll(((ShapedRecipeBlueprint) recipe).getSlotIngredients().values());
        } else if (recipe instanceof ShapelessRecipeBlueprint) {
            ingredients.addAll(((ShapelessRecipeBlueprint) recipe).getRequiredIngredients());
        }

        for (IngredientBlueprint bp : ingredients) {
            // [오류 수정됨] CustomItemUtil.getItemId 사용
            String itemId = CustomItemUtil.getItemId(bp.getItemStack()); // bp.getItemStack()은 수량이 1임
            
            if (itemId == null) {
                // 바닐라 재료
                itemId = bp.getItemStack().getType().name();
            }
            
            requiredCounts.put(itemId, requiredCounts.getOrDefault(itemId, 0) + bp.getAmount());
        }

        // 플레이어 재료 수량과 비교
        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            String itemId = entry.getKey();
            int requiredAmount = entry.getValue();
            if (playerIngredients.getOrDefault(itemId, 0) < requiredAmount) {
                return false; // 재료 부족
            }
        }
        return true; // 재료 충분
    }


    @Override
    public Inventory getInventory() {
        return this.gui;
    }

    // [신규 추가] RecipeBookListener가 현재 페이지를 알 수 있도록 getter 추가
    public int getPage() {
        return this.page;
    }

    // [신규 추가] RecipeBookListener가 최대 페이지를 알 수 있도록 getter 추가
    public int getMaxPage() {
        return this.maxPage;
    }
}