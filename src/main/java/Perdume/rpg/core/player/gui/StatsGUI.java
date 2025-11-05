package Perdume.rpg.core.player.gui;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.player.data.PlayerData;
import Perdume.rpg.core.player.stats.PlayerStats;
import Perdume.rpg.core.player.stats.StatType;
import Perdume.rpg.core.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * [전면 수정] 플레이어의 캐릭터 정보(순수 스탯)를 보여주는 GUI를 생성하고 관리하는 클래스입니다.
 */
public class StatsGUI {

    private final Rpg plugin;
    private final Player player;
    private final Inventory inventory;
    private final PlayerData playerData;

    public StatsGUI(Rpg plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = plugin.getPlayerDataManager().getPlayerData(player);
        this.inventory = Bukkit.createInventory(null, 54, "§8[ 캐릭터 정보 ]");

        initializeItems();
    }

    private void initializeItems() {
        ItemStack background = new ItemFactory(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, background);
        }

        PlayerStats stats = playerData.getStats();
        if (stats == null) return;

        inventory.setItem(13, new ItemFactory(Material.PLAYER_HEAD)
                .setDisplayName("§a" + player.getName() + "님의 정보")
                .setLore(
                        "§7--------------------",
                        "§f레벨: §e" + stats.getLevel(),
                        "§f스탯 포인트: §b" + stats.getStatPoints(),
                        "§7--------------------"
                ).build());

        // --- 각 스탯 아이템 배치 ---
        inventory.setItem(29, createStatItem(StatType.STRENGTH));
        inventory.setItem(30, createStatItem(StatType.AGILITY));
        inventory.setItem(31, createStatItem(StatType.INTELLIGENCE));
        inventory.setItem(32, createStatItem(StatType.VITALITY));
        inventory.setItem(33, createStatItem(StatType.PIERCING));
    }

    private ItemStack createStatItem(StatType type) {
        PlayerStats stats = playerData.getStats();
        int baseValue = stats.getStat(type); // 플레이어가 투자한 순수 스탯 값

        return new ItemFactory(type.getIcon())
                .setDisplayName("§l§a" + type.getDisplayName())
                .setLore(
                        "§7" + type.getDescription(),
                        "§7--------------------",
                        "§f투자한 포인트: §e" + baseValue,
                        "§7--------------------",
                        stats.getStatPoints() > 0 ? "§e좌클릭: §a+1 포인트 투자" : "§c투자할 포인트가 없습니다.",
                        stats.getStatPoints() > 0 ? "§eShift+좌클릭: §a+10 포인트 투자" : ""
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    public void open() {
        player.openInventory(inventory);
    }
}

