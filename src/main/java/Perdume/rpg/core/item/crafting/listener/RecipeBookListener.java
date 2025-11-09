package Perdume.rpg.core.item.crafting.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.crafting.RecipeManager;
import Perdume.rpg.core.item.crafting.gui.CustomCraftGUI;
import Perdume.rpg.core.item.crafting.gui.RecipeBookGUI;
import Perdume.rpg.core.item.crafting.gui.RecipeViewerGUI;
import Perdume.rpg.core.item.crafting.recipe.CustomRecipe;
import org.bukkit.ChatColor; // [추가] ChatColor import
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
// [제거] PersistentDataContainer
// [제거] PersistentDataType

import java.util.List; // [추가] List import
// [제거] Map

/**
 * RecipeBookGUI와 RecipeViewerGUI의 클릭 이벤트를 처리하는 리스너입니다.
 * [최종 수정] 레시피 ID 파싱 방식을 NBT에서 Lore로 변경, 뒤로가기 버튼 슬롯 수정
 */
public class RecipeBookListener implements Listener {

    private final Rpg plugin;
    private final RecipeManager recipeManager;

    public RecipeBookListener(Rpg plugin) {
        this.plugin = plugin;
        this.recipeManager = plugin.getRecipeManager();
    }

    @EventHandler
    public void onRecipeBookClick(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null || !(clickedInv.getHolder() instanceof RecipeBookGUI)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        RecipeBookGUI gui = (RecipeBookGUI) clickedInv.getHolder();
        int slot = event.getRawSlot();

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        // --- 페이지 넘기기 ---
        // 이전 페이지 (슬롯 45)
        if (slot == 45) { // [수정] 48번 -> 45번
            if (gui.getPage() > 1) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                new RecipeBookGUI(plugin, recipeManager, player).open(gui.getPage() - 1);
            }
            return;
        }

        // 뒤로가기 (제작대) (슬롯 48)
        if (slot == 48) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            new CustomCraftGUI(plugin).open(player); // 제작대 GUI 열기
            return;
        }

        // 다음 페이지 (슬롯 53)
        if (slot == 53) { // [수정] 50번 -> 53번
            if (gui.getPage() < gui.getMaxPage()) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                new RecipeBookGUI(plugin, recipeManager, player).open(gui.getPage() + 1);
            }
            return;
        }

        // --- 레시피 클릭 (상세보기) ---
        // 테두리(45~53)를 제외한 0~44 슬롯
        if (slot >= 0 && slot <= 44) {
            if (clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                return;
            }

            // [버그 수정] NBT 대신 Lore에서 Recipe ID 추출
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasLore()) return;

            String recipeId = null;
            List<String> lore = meta.getLore();
            for (String line : lore) {
                String strippedLine = ChatColor.stripColor(line); // 색상 코드 제거
                if (strippedLine.startsWith("Recipe ID: ")) {
                    recipeId = strippedLine.substring("Recipe ID: ".length());
                    break;
                }
            }

            if (recipeId != null) {
                CustomRecipe recipe = recipeManager.getRecipes().get(recipeId); // RecipeManager에서 ID로 레시피 조회

                if (recipe != null) {
                    // 레시피 뷰어 GUI 열기
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new RecipeViewerGUI(plugin, recipeManager, player, recipe, gui.getPage()).open();
                } else {
                    player.sendMessage("§c오류: 알 수 없는 레시피 ID입니다. (" + recipeId + ")");
                }
            }
            // [제거] NBT 관련 로직
        }
    }

    @EventHandler
    public void onRecipeViewerClick(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null || !(clickedInv.getHolder() instanceof RecipeViewerGUI)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        RecipeViewerGUI gui = (RecipeViewerGUI) clickedInv.getHolder();
        int slot = event.getRawSlot();

        // [버그 수정] 뒤로가기 버튼 (49번 슬롯 -> 45번 슬롯)
        if (slot == 45) { // 49번에서 45번으로 수정 (RecipeViewerGUI.java의 BACK_BUTTON_SLOT)
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            new RecipeBookGUI(plugin, recipeManager, player).open(gui.getPreviousPage());
        }
    }
}