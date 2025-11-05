package Perdume.rpg.gamemode.island.listener;

import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.gui.IslandDetailSettingsGUI;
import Perdume.rpg.gamemode.island.gui.IslandMemberGUI;
import Perdume.rpg.gamemode.island.gui.IslandUpgradeGUI;
import Perdume.rpg.system.SkyblockManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * [리팩토링됨] 메인 섬 설정 GUI의 클릭 이벤트를 처리하는 리스너입니다.
 * 새로운 Island/IslandData 구조에 맞게 수정되었습니다.
 */
public class IslandSettingsGUIListener implements Listener {

    private final SkyblockManager skyblockManager;

    public IslandSettingsGUIListener(SkyblockManager skyblockManager) {
        this.skyblockManager = skyblockManager;
    }

    @EventHandler
    public void onSettingsGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§8[ 섬 설정 ]")) { // GUI 제목으로 구분
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 플레이어가 속한 로드된 섬을 가져옵니다.
        Island island = skyblockManager.getLoadedIsland(player);
        if (island == null) {
            player.sendMessage("§c섬 정보를 찾을 수 없습니다. 섬에 입장한 후 다시 시도해주세요.");
            player.closeInventory();
            return;
        }

        // 클릭한 아이템의 종류에 따라 다른 GUI를 열어줍니다.
        switch (clickedItem.getType()) {
            case PLAYER_HEAD:
                // new IslandMemberGUI(island).open(player); // 멤버 관리 GUI 열기
                player.sendMessage("§e[WIP] 멤버 관리 기능이 준비 중입니다.");
                break;
            case ANVIL:
                new IslandUpgradeGUI(island).open(player); // 업그레이드 GUI 열기
                break;
            case COMPARATOR:
                // new IslandDetailSettingsGUI(island).open(player); // 상세 설정 GUI 열기
                player.sendMessage("§e[WIP] 상세 설정 기능이 준비 중입니다.");
                break;
            case BARRIER:
                player.closeInventory();
                break;
        }
    }
}
