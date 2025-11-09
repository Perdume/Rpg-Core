package Perdume.rpg.core.item.crafting.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.CustomItemUtil; // [신규] CustomItemUtil import
import Perdume.rpg.core.item.crafting.RecipeManager;
import Perdume.rpg.core.item.crafting.gui.CustomCraftGUI;
import Perdume.rpg.core.item.crafting.gui.RecipeBookGUI;
import Perdume.rpg.core.item.crafting.recipe.CustomRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomCraftGUI (커스텀 제작대)의 모든 상호작용을 처리하는 리스너입니다.
 * [최종 수정] 커스텀 아이템이 바닐라 조합에 사용되는 버그 수정
 */
public class CustomCraftGUIListener implements Listener {

    private final Rpg plugin;
    private final RecipeManager recipeManager;

    // GUI 슬롯 정의
    private final List<Integer> craftingSlots = List.of(
            10, 11, 12, 19, 20, 21, 28, 29, 30
    );
    private final int RESULT_SLOT = 24;
    private final int RECIPE_BOOK_SLOT = 8;
    // 3x3 + 1칸 외의 모든 슬롯
    private final List<Integer> borderSlots = List.of(
            0, 1, 2, 3, 4, 5, 6, 7, /* 8번은 레시피북 */ 9,
            13, 14, 15, 16, 17, 18,
            22, 23, /* 24번은 결과 */ 25, 26, 27,
            31, 32, 33, 34, 35, 36,
            37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    );
    // 플레이어 인벤토리 슬롯 (36칸)
    private final int PLAYER_INV_START = 54;
    private final int PLAYER_INV_END = 89;

    public CustomCraftGUIListener(Rpg plugin) {
        this.plugin = plugin;
        this.recipeManager = plugin.getRecipeManager();
    }

    /**
     * [버그 수정] 아이템을 "드래그"로 놓았을 때 갱신
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGuiDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getHolder() == null || !(topInv.getHolder() instanceof CustomCraftGUI)) {
            return;
        }

        CustomCraftGUI gui = (CustomCraftGUI) topInv.getHolder();

        boolean affectsCrafting = false;
        for (int slot : event.getRawSlots()) {
            if (craftingSlots.contains(slot)) {
                affectsCrafting = true;
                break;
            }
        }

        if (affectsCrafting) {
            updateCraftingResult(gui);
        }
    }


    /**
     * 커스텀 제작 GUI 클릭 이벤트 처리
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onGuiClick(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null || !(event.getView().getTopInventory().getHolder() instanceof CustomCraftGUI)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        CustomCraftGUI gui = (CustomCraftGUI) event.getView().getTopInventory().getHolder();
        int slot = event.getRawSlot();

        // 1. 결과물 슬롯 클릭 (Shift 클릭 / 일반 클릭)
        if (slot == RESULT_SLOT) {
            event.setCancelled(true);
            ItemStack resultItem = gui.getInventory().getItem(RESULT_SLOT);
            if (resultItem == null || resultItem.getType().isAir() || resultItem.isSimilar(gui.getNoRecipeItem())) {
                return; // 방벽이거나 비어있으면 무시
            }

            ItemStack[] matrix = createCraftingMatrix(gui);
            CustomRecipe customRecipe = findCustomRecipe(matrix);

            // [수정됨] 바닐라 레시피는 여기서 다시 찾습니다. (커스텀 아이템이 섞였는지 확인 필요)
            Recipe vanillaRecipe = null;
            if (customRecipe == null && !matrixContainsCustomItem(matrix)) {
                vanillaRecipe = Bukkit.getServer().getCraftingRecipe(matrix, player.getWorld());
            }

            if (event.isShiftClick()) {
                handleShiftCraft(player, gui, matrix, customRecipe, vanillaRecipe);
            } else {
                handleNormalCraft(player, gui, matrix, customRecipe, vanillaRecipe, event.getCursor());
            }
            updateCraftingResult(gui);
            return;
        }

        // 2. 레시피북 슬롯 클릭
        if (slot == RECIPE_BOOK_SLOT) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            new RecipeBookGUI(plugin, recipeManager, player).open(1);
            return;
        }

        // 3. 테두리 슬롯 클릭
        if (borderSlots.contains(slot)) {
            event.setCancelled(true);
            return;
        }

        // 4. 조합 슬롯 <-> 인벤토리 상호작용
        if ((craftingSlots.contains(slot)) || (slot >= PLAYER_INV_START && slot <= PLAYER_INV_END)) {
            // Shift 클릭
            if (event.isShiftClick()) {
                updateCraftingResult(gui);
                return;
            }
            // 일반 클릭 및 드래그 (cancel=false)
            updateCraftingResult(gui);
            return;
        }
    }

    /**
     * GUI가 닫힐 때 아이템 반환
     */
    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomCraftGUI)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        CustomCraftGUI gui = (CustomCraftGUI) event.getInventory().getHolder();
        ItemStack[] matrix = createCraftingMatrix(gui);

        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }

    /**
     * 1틱 지연 후 조합 결과 슬롯을 갱신합니다.
     */
    private void updateCraftingResult(CustomCraftGUI gui) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack[] matrix = createCraftingMatrix(gui);
                CustomRecipe customRecipe = findCustomRecipe(matrix);

                if (customRecipe != null) {
                    gui.getInventory().setItem(RESULT_SLOT, customRecipe.getResult());
                } else {
                    // [버그 수정]
                    // 조합창에 커스텀 아이템이 하나라도 있는지 확인
                    if (matrixContainsCustomItem(matrix)) {
                        // 커스텀 아이템이 섞여있으면, 바닐라 조합을 시도하지 않고 "조합 불가" 처리
                        gui.getInventory().setItem(RESULT_SLOT, gui.getNoRecipeItem());
                    } else {
                        // 모든 아이템이 바닐라이므로, 바닐라 레시피 확인
                        Recipe vanillaRecipe = Bukkit.getServer().getCraftingRecipe(matrix, Bukkit.getWorlds().get(0));
                        if (vanillaRecipe != null) {
                            gui.getInventory().setItem(RESULT_SLOT, vanillaRecipe.getResult());
                        } else {
                            // 둘 다 없으면 방벽
                            gui.getInventory().setItem(RESULT_SLOT, gui.getNoRecipeItem());
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 1L); // 1틱 딜레이
    }

    /**
     * Shift 클릭 (최대치 조합)을 처리합니다.
     */
    private void handleShiftCraft(Player player, CustomCraftGUI gui, ItemStack[] matrix, CustomRecipe customRecipe, Recipe vanillaRecipe) {
        if (customRecipe == null && vanillaRecipe == null) return;

        ItemStack resultItem = (customRecipe != null) ? customRecipe.getResult() : vanillaRecipe.getResult();
        int maxCraftAmount = 64; // 최대 제작 가능 횟수 (재료 기반)

        // 1. 재료를 기반으로 최대 몇 번 제작 가능한지 계산
        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                maxCraftAmount = Math.min(maxCraftAmount, item.getAmount());
            }
        }
        if (maxCraftAmount == 0) return;

        // 2. 결과물을 인벤토리에 넣을 수 있는 만큼 계산
        int craftedAmount = 0;
        for (int i = 0; i < maxCraftAmount; i++) {
            ItemStack craftResult = resultItem.clone();
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(craftResult);
            if (!leftover.isEmpty()) {
                // 인벤토리 꽉 참
                break;
            }
            craftedAmount++;
        }

        // 3. 제작한 횟수(craftedAmount)만큼 재료 소모
        if (craftedAmount > 0) {
            for (int slot : craftingSlots) {
                ItemStack item = gui.getInventory().getItem(slot);
                if (item != null && !item.getType().isAir()) {
                    item.setAmount(item.getAmount() - craftedAmount); // null 체크 필요 없음
                }
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }
    }

    /**
     * 일반 클릭 (1회 조합)을 처리합니다.
     */
    private void handleNormalCraft(Player player, CustomCraftGUI gui, ItemStack[] matrix, CustomRecipe customRecipe, Recipe vanillaRecipe, ItemStack cursorItem) {
        if (customRecipe == null && vanillaRecipe == null) return;

        ItemStack resultItem = (customRecipe != null) ? customRecipe.getResult() : vanillaRecipe.getResult();

        // 커서에 아이템이 없거나, 결과물과 동일한 아이템인 경우
        if (cursorItem == null || cursorItem.getType().isAir()) {
            player.setItemOnCursor(resultItem.clone());
        } else if (cursorItem.isSimilar(resultItem) && (cursorItem.getAmount() + resultItem.getAmount()) <= cursorItem.getMaxStackSize()) {
            cursorItem.setAmount(cursorItem.getAmount() + resultItem.getAmount());
        } else {
            return; // 커서에 다른 아이템이 있거나, 꽉 차서 조합 불가
        }

        // 재료 1개씩 소모
        for (int slot : craftingSlots) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && !item.getType().isAir()) {
                item.setAmount(item.getAmount() - 1);
            }
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
    }


    /**
     * GUI 인벤토리에서 3x3 매트릭스(ItemStack[9])를 추출합니다.
     */
    private ItemStack[] createCraftingMatrix(CustomCraftGUI gui) {
        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = gui.getInventory().getItem(craftingSlots.get(i));
        }
        return matrix;
    }

    /**
     * 매트릭스와 일치하는 "커스텀" 레시피를 찾습니다.
     */
    private CustomRecipe findCustomRecipe(ItemStack[] matrix) {
        for (Map.Entry<String, CustomRecipe> entry : recipeManager.getRecipes().entrySet()) {
            CustomRecipe recipe = entry.getValue();
            if (recipe.matches(matrix)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * [신규] 매트릭스에 커스텀 아이템이 하나라도 있는지 확인합니다.
     */
    private boolean matrixContainsCustomItem(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (CustomItemUtil.isCustomItem(item)) {
                return true;
            }
        }
        return false;
    }
}