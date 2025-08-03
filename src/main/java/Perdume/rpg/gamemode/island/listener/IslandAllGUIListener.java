package Perdume.rpg.gamemode.island.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.gui.IslandDetailSettingsGUI;
import Perdume.rpg.gamemode.island.gui.IslandMemberGUI;
import Perdume.rpg.system.SkyblockManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IslandAllGUIListener implements Listener {

    private final Rpg plugin;
    private final SkyblockManager skyblockManager;
    private final Set<UUID> invitingPlayers = new HashSet<>();

    public IslandAllGUIListener(Rpg plugin) {
        this.plugin = plugin;
        this.skyblockManager = plugin.getSkyblockManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§8[섬")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // --- 1. 멤버 관리 GUI ---
        if (title.equals(IslandMemberGUI.GUI_TITLE)) {
            Island island = skyblockManager.getIsland(player);
            if (island == null) return;

            // 초대 버튼
            if (clickedItem.getType() == Material.WRITABLE_BOOK) {
                player.closeInventory();
                player.sendMessage("§a초대할 플레이어의 이름을 채팅으로 입력해주세요.");
                invitingPlayers.add(player.getUniqueId());
            }
            // 멤버 머리 클릭 (추방)
            else if (clickedItem.getType() == Material.PLAYER_HEAD) {
                OfflinePlayer target = ((org.bukkit.inventory.meta.SkullMeta) clickedItem.getItemMeta()).getOwningPlayer();
                if (target != null) {
                    skyblockManager.kickFromIsland(player, target.getPlayer());
                }
            }
        }
        // --- 2. 상세 설정 GUI ---
        else if (title.equals(IslandDetailSettingsGUI.GUI_TITLE)) {
            Island island = skyblockManager.getIsland(player);
            if (island == null) return;

            switch (clickedItem.getType()) {
                case WATER_BUCKET -> island.getWorld().setStorm(false); // 날씨 맑음
                case CLOCK -> island.getWorld().setTime(6000); // 시간 정오
            }
            player.sendMessage("§a섬 설정을 변경했습니다.");
            player.closeInventory();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!invitingPlayers.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        String targetName = event.getMessage();
        Player target = Bukkit.getPlayer(targetName);

        Bukkit.getScheduler().runTask(plugin, () -> {
            skyblockManager.inviteToIsland(player, target);
        });
        invitingPlayers.remove(player.getUniqueId());
    }
}