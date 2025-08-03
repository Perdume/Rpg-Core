package Perdume.rpg.listener;
import Perdume.rpg.Rpg;
import Perdume.rpg.config.LocationManager;
import Perdume.rpg.gui.SpawnGUI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SpawnGUIListener implements Listener {
    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(SpawnGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // 비어있는 곳이나, '잠긴' 아이템(회색 염료)을 클릭하면 무시
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_DYE) {
            return;
        }

        // 아이템에 숨겨진 'spawn_id' 정보를 읽어옵니다.
        String spawnId = clickedItem.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(Rpg.getInstance(), "spawn_id"), PersistentDataType.STRING);

        if (spawnId != null) {
            // [핵심 수정] ConfigManager 대신, LocationManager를 통해 위치 정보를 가져옵니다.
            Location loc = LocationManager.getSpawnLocation(spawnId);

            if (loc != null) {
                player.teleport(loc);
                player.sendMessage("§a" + clickedItem.getItemMeta().getDisplayName() + "§a(으)로 이동했습니다.");
            } else {
                player.sendMessage("§c해당 위치를 찾을 수 없습니다. 관리자에게 문의해주세요.");
            }
            player.closeInventory();
        }
    }
}