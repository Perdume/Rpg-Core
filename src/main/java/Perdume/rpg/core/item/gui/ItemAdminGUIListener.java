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

        // --- [디버그] 아이템 지급 로직 (NBT 덤프 포함) ---
        if (event.getRawSlot() < 45) {
            // [수정] 템플릿을 복제하는 대신, '인스턴스'를 생성하여 지급합니다.
            String itemId = itemManager.getItemId(clickedItem);
            if (itemId == null) return; // 알 수 없는 아이템

            int amount = event.isShiftClick() ? 64 : 1;

            // 1. [핵심] createItemInstance가 아이템을 생성합니다.
            ItemStack itemToGive = itemManager.createItemInstance(itemId, 1);

            // --- [신규 디버그 로직] ---
            Rpg.log.info("--- [디버그/지급] createItemInstance() 직후 NBT 덤프 ---");
            Rpg.log.info("[디버그/지급] 아이템: " + itemId);
            if (itemToGive != null && itemToGive.hasItemMeta()) {
                // 2. Bukkit API를 사용하여 생성된 아이템의 모든 메타데이터(NBT/PDC 포함)를 문자열로 변환
                String metaString = itemToGive.getItemMeta().serialize().toString();
                Rpg.log.info("[디버그/지급] [NBT 데이터]: " + metaString);
            } else {
                Rpg.log.warning("[디버그/지급] [NBT 데이터]: (알 수 없음 - ItemMeta가 null입니다)");
            }
            Rpg.log.info("--- [디버그/지급] NBT 덤프 완료 ---");


            if (amount == 64) {
                itemToGive.setAmount(64); // 수량만 64개로 변경
                player.getInventory().addItem(itemToGive);
            } else {
                player.getInventory().addItem(itemToGive);
            }
        }
    }
}
