package Perdume.rpg.gamemode.island.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * '섬 보관함' GUI에서 플레이어의 행동을 감시하여,
 * 아이템을 넣는 행위는 막고 꺼내는(회수) 행위만 허용하는 리스너입니다.
 */
public class IslandStorageGUIListener implements Listener {

    @EventHandler
    public void onStorageClick(InventoryClickEvent event) {
        // GUI의 제목이 "섬 보관함"이 아니면 무시합니다.
        if (!event.getView().getTitle().equals("섬 보관함")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        // 클릭한 인벤토리가 없거나, '섬 보관함'(상단 인벤토리)이 아니면 로직을 실행할 필요가 없습니다.
        // (플레이어가 자기 인벤토리를 클릭하는 것은 허용)
        if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
            // 예외: Shift+Click은 아이템을 아래 -> 위로 옮기므로 별도 처리
            if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY){
                // 아이템을 '섬 보관함'으로 옮기려는 시도이므로 취소합니다.
                event.setCancelled(true);
                player.sendMessage("§c이 보관함에는 아이템을 넣을 수 없습니다. 회수만 가능합니다.");
            }
            return;
        }

        // --- 이 아래는 '섬 보관함'(상단 인벤토리)을 클릭했을 때의 로직 ---

        // 아이템을 집어서 보관함에 내려놓는 모든 행위를 막습니다.
        switch (event.getAction()) {
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR:
                event.setCancelled(true);
                player.sendMessage("§c이 보관함에는 아이템을 넣을 수 없습니다. 회수만 가능합니다.");
                break;
            default:
                // PICKUP_ALL, PICKUP_HALF 등 아이템을 가져가는 행위는 허용 (아무것도 하지 않음)
                break;
        }
    }
}
