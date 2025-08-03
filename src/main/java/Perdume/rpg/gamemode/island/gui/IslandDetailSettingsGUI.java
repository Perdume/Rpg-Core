package Perdume.rpg.gamemode.island.gui;

import Perdume.rpg.gamemode.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class IslandDetailSettingsGUI {

    public static final String GUI_TITLE = "§8[섬 상세 설정]";

    public static void open(Player player, Island island) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // 날씨 설정 버튼
        ItemStack weatherItem = new ItemStack(Material.WATER_BUCKET);
        ItemMeta weatherMeta = weatherItem.getItemMeta();
        weatherMeta.setDisplayName("§e[ 날씨 설정 ]");
        weatherMeta.setLore(List.of("§7섬의 날씨를 변경합니다."));
        weatherItem.setItemMeta(weatherMeta);
        gui.setItem(11, weatherItem);

        // 시간 설정 버튼
        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta timeMeta = timeItem.getItemMeta();
        timeMeta.setDisplayName("§d[ 시간 설정 ]");
        timeMeta.setLore(List.of("§7섬의 시간을 변경합니다."));
        timeItem.setItemMeta(timeMeta);
        gui.setItem(15, timeItem);

        player.openInventory(gui);
    }
}