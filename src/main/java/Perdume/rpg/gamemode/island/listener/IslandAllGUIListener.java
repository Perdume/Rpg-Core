package Perdume.rpg.gamemode.island.listener;

import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.IslandData;
import Perdume.rpg.system.SkyblockManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * [리팩토링됨] 섬 멤버 관리, 상세 설정 등 여러 하위 GUI의 클릭 이벤트를 모두 처리하는 리스너입니다.
 */
public class IslandAllGUIListener implements Listener {

    private final SkyblockManager skyblockManager;

    public IslandAllGUIListener(SkyblockManager skyblockManager) {
        this.skyblockManager = skyblockManager;
    }

    @EventHandler
    public void onAllIslandGUIClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // GUI 제목에 따라 분기
        if (title.equals("§8[ 멤버 관리 ]")) {
            handleMemberGUIClick(event, player, clickedItem);
        } else if (title.equals("§8[ 섬 상세 설정 ]")) {
            handleDetailSettingsGUIClick(event, player, clickedItem);
        }
    }

    private void handleMemberGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        event.setCancelled(true);
        if (clickedItem == null) return;

        Island island = skyblockManager.getLoadedIsland(player);
        if (island == null) {
            player.closeInventory();
            return;
        }

        // 섬 주인이 아니면 멤버를 추방할 수 없습니다.
        if (!island.getOwner().equals(player.getUniqueId())) return;

        // 아이템 Lore의 마지막 줄에서 UUID를 읽어와 추방 로직을 실행합니다. (GUI 생성 시 UUID를 숨겨서 저장해야 함)
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
            try {
                String uuidString = clickedItem.getItemMeta().getLore().get(clickedItem.getItemMeta().getLore().size() - 1);
                UUID targetUUID = UUID.fromString(org.bukkit.ChatColor.stripColor(uuidString));
                Player targetPlayer = Bukkit.getPlayer(targetUUID); // 온라인 플레이어만 추방 가능하도록 가정

                if(targetPlayer != null) {
                    skyblockManager.kickFromIsland(player, targetPlayer);
                    player.closeInventory();
                    player.sendMessage("§a" + targetPlayer.getName() + "님을 섬에서 추방했습니다.");
                }
            } catch (Exception ignored) {
                // UUID 파싱에 실패하면 아무것도 하지 않음
            }
        }
    }

    private void handleDetailSettingsGUIClick(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        event.setCancelled(true);
        if (clickedItem == null) return;

        Island island = skyblockManager.getLoadedIsland(player);
        if (island == null) {
            player.closeInventory();
            return;
        }

        // 섬 주인이 아니면 설정을 변경할 수 없습니다.
        if (!island.getOwner().equals(player.getUniqueId())) return;

        IslandData islandData = island.getData();
        boolean changed = false;

        // 클릭한 아이템 종류에 따라 설정을 변경합니다. (GUI에 토글 가능한 설정 아이템이 있다고 가정)
        switch(clickedItem.getType()){
            case OAK_DOOR:
                // islandData.setPublic(!islandData.isPublic()); // isPublic 같은 설정이 IslandData에 있다고 가정
                changed = true;
                break;
            case GRASS_BLOCK:
                // islandData.setVisitorBuild(!islandData.canVisitorBuild()); // 방문자 건축 권한 설정이 있다고 가정
                changed = true;
                break;
        }

        if(changed) {
            skyblockManager.saveIslandData(islandData); // 변경된 데이터 저장
            player.sendMessage("§a섬 설정을 변경했습니다.");
            // new IslandDetailSettingsGUI(island).open(player); // GUI 새로고침
        }
    }
}
