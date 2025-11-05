package Perdume.rpg.core.item.gui;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.ItemManager;
import Perdume.rpg.core.item.gui.ItemAdminGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * ItemAdminGUI의 클릭 이벤트를 처리하여 아이템을 지급하고 페이지를 넘기는 리스너입니다.
 */
public class ItemAdminGUIListener implements Listener {

    private final Rpg plugin;
    private final ItemManager itemManager;

    public ItemAdminGUIListener(Rpg plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    @EventHandler
    public void onItemAdminGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("§8[커스텀 아이템 목록]")) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 페이지 정보 파싱
        String title = event.getView().getTitle();
        int currentPage = Integer.parseInt(title.substring(title.indexOf("(") + 1, title.indexOf("/")));

        // 페이지 이동 버튼 처리
        if (event.getRawSlot() == 45 && clickedItem.getType() == Material.ARROW) { // 이전 페이지
            new ItemAdminGUI(itemManager, currentPage - 1).open(player);
            return;
        }
        if (event.getRawSlot() == 53 && clickedItem.getType() == Material.ARROW) { // 다음 페이지
            new ItemAdminGUI(itemManager, currentPage + 1).open(player);
            return;
        }

        // 아이템 지급 로직
        if (event.getRawSlot() < 45) {
            ItemStack itemToGive = clickedItem.clone();
            if (event.isShiftClick()) {
                itemToGive.setAmount(64);
            } else {
                itemToGive.setAmount(1);
            }
            player.getInventory().addItem(itemToGive);
        }
    }
}
