package Perdume.rpg.gamemode.island.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class IslandSettingsGUI {

    public static final String GUI_TITLE = "§8[섬 설정]";

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // 홈 설정 버튼
        ItemStack homeItem = new ItemStack(Material.RED_BED);
        ItemMeta homeMeta = homeItem.getItemMeta();
        homeMeta.setDisplayName("§a[ 홈 위치 설정 ]");
        homeMeta.setLore(List.of("§7현재 위치를 섬의 스폰 지점으로 설정합니다."));
        homeItem.setItemMeta(homeMeta);
        gui.setItem(11, homeItem);
        
        // 멤버 관리 버튼
        ItemStack membersItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta membersMeta = membersItem.getItemMeta();
        membersMeta.setDisplayName("§b[ 멤버 관리 ]");
        membersMeta.setLore(List.of("§7섬에 방문하거나 건축할 멤버를 관리합니다."));
        membersItem.setItemMeta(membersMeta);
        gui.setItem(13, membersItem);

        // 섬 설정 버튼 (날씨, 시간 등)
        ItemStack settingsItem = new ItemStack(Material.COMPARATOR);
        ItemMeta settingsMeta = settingsItem.getItemMeta();
        settingsMeta.setDisplayName("§e[ 섬 상세 설정 ]");
        settingsMeta.setLore(List.of("§7섬의 날씨, 시간 등 다양한 설정을 변경합니다."));
        settingsItem.setItemMeta(settingsMeta);
        gui.setItem(15, settingsItem);

        player.openInventory(gui);
    }
}