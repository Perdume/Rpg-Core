package Perdume.rpg.gamemode.island.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.gui.IslandDetailSettingsGUI;
import Perdume.rpg.gamemode.island.gui.IslandMemberGUI;
import Perdume.rpg.gamemode.island.gui.IslandSettingsGUI;
import Perdume.rpg.system.SkyblockManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class IslandSettingsGUIListener implements Listener {

    private final Rpg plugin;
    private final SkyblockManager skyblockManager;

    public IslandSettingsGUIListener(Rpg plugin) {
        this.plugin = plugin;
        this.skyblockManager = plugin.getSkyblockManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(IslandSettingsGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Island island = skyblockManager.getIsland(player);
        if (island == null) {
            player.sendMessage("§c당신은 섬을 소유하고 있지 않습니다.");
            player.closeInventory();
            return;
        }

        switch (clickedItem.getType()) {
            case RED_BED:
                // 홈 설정 로직
                skyblockManager.setHome(player);
                player.closeInventory();
                break;
            case PLAYER_HEAD:
                // [핵심] 멤버 관리 GUI 열기
                IslandMemberGUI.open(player, island);
                break;
            case COMPARATOR:
                // [핵심] 섬 상세 설정 GUI 열기
                IslandDetailSettingsGUI.open(player, island);
                break;
            default:
                break;
        }
    }
}