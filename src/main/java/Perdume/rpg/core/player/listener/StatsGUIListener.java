package Perdume.rpg.core.player.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.Manager.StatsManager;
import Perdume.rpg.core.player.data.PlayerData;
import Perdume.rpg.core.player.gui.StatsGUI;
import Perdume.rpg.core.player.stats.PlayerStats;
import Perdume.rpg.core.player.stats.StatType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * [전면 수정] StatsGUI에서 스탯 투자 이벤트를 처리하고,
 * StatsManager를 호출하여 AuraSkills에 환산값을 업데이트하는 리스너입니다.
 */
public class StatsGUIListener implements Listener {

    private final Rpg plugin;
    private final StatsManager statsManager;

    public StatsGUIListener(Rpg plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getStatsManager();
    }

    @EventHandler
    public void onStatsGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§8[ 캐릭터 정보 ]")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) return;

        StatType clickedStat = null;
        for (StatType type : StatType.values()) {
            if (type.getIcon() == clickedItem.getType()) {
                clickedStat = type;
                break;
            }
        }

        if (clickedStat == null) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        PlayerStats stats = playerData.getStats();
        if (stats == null) return;

        int pointsToSpend = 0;
        if (event.getClick() == ClickType.LEFT) pointsToSpend = 1;
        else if (event.getClick() == ClickType.SHIFT_LEFT) pointsToSpend = 10;

        if (pointsToSpend > 0) {
            if (stats.getStatPoints() >= pointsToSpend) {
                // 1. 순수 스탯을 올립니다.
                stats.setStat(clickedStat, stats.getStat(clickedStat) + pointsToSpend);
                stats.setStatPoints(stats.getStatPoints() - pointsToSpend);

                // 2. [핵심] StatsManager를 호출하여 변경된 스탯을 AuraSkills에 환산 적용합니다.
                statsManager.updateAllStatModifiers(player);

                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);

                // 3. GUI를 새로고침합니다.
                new StatsGUI(plugin, player).open();
            } else {
                player.sendMessage("§c스탯 포인트가 부족합니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }
}

