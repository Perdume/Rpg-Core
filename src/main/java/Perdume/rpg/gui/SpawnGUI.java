// gui/SpawnGUI.java
package Perdume.rpg.gui;
import Perdume.rpg.Rpg;
import Perdume.rpg.config.LocationManager;
import Perdume.rpg.core.player.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SpawnGUI {
    public static final String GUI_TITLE = "§8[마을 이동]";

    public static void open(Player player) {
        // GUI의 크기를 스폰 지점 수에 맞게 동적으로 조절 (최대 54칸)
        int guiSize = (int) (Math.ceil(LocationManager.getSpawnLocations().size() / 9.0) * 9);
        Inventory gui = Bukkit.createInventory(null, Math.max(9, guiSize), GUI_TITLE);

        Rpg plugin = Rpg.getInstance();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        FileConfiguration locationsConfig = LocationManager.getLocationsConfig();

        ConfigurationSection section = locationsConfig.getConfigurationSection("spawn-locations");

        if (section == null || section.getKeys(false).isEmpty()) {
            ItemStack noSpawnItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = noSpawnItem.getItemMeta();
            meta.setDisplayName("§c등록된 스폰 지점이 없습니다.");
            meta.setLore(List.of("§7관리자가 아직 스폰 지점을 설정하지 않았습니다."));
            noSpawnItem.setItemMeta(meta);
            gui.setItem(4, noSpawnItem);
        } else {
            for (String key : section.getKeys(false)) {
                // --- [핵심] 플레이어의 데이터와 비교하여 아이템을 다르게 생성 ---
                if (data.hasUnlockedSpawn(key)) {
                    // [해금됨]
                    // yml 파일에 icon이 없으면 기본값으로 GRASS_BLOCK 사용
                    String iconMaterialName = section.getString(key + ".icon", "GRASS_BLOCK");
                    Material iconMaterial = Material.matchMaterial(iconMaterialName);
                    if (iconMaterial == null) iconMaterial = Material.STONE; // 잘못된 이름일 경우 돌로 표시

                    ItemStack item = new ItemStack(iconMaterial);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) continue;

                    meta.setDisplayName(section.getString(key + ".display-name", "§a" + key));
                    meta.setLore(section.getStringList(key + ".lore"));
                    // 아이템에 마을 ID를 숨겨둠 (SpawnGUIListener에서 사용)
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "spawn_id"), PersistentDataType.STRING, key);
                    item.setItemMeta(meta);
                    gui.addItem(item);

                } else {
                    // [잠김]
                    ItemStack item = new ItemStack(Material.GRAY_DYE);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) continue;

                    meta.setDisplayName("§8???");
                    meta.setLore(List.of("§7아직 발견하지 못한 지역입니다."));
                    item.setItemMeta(meta);
                    gui.addItem(item);
                }
            }
        }
        player.openInventory(gui);
    }
}

// listener/SpawnGUIListener.java