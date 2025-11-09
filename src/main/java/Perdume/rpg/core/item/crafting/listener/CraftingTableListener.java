package Perdume.rpg.core.item.crafting.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.crafting.gui.CustomCraftGUI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 플레이어가 제작대를 우클릭하는 이벤트를 감지하여
 * 바닐라 GUI 대신 커스텀 제작 GUI를 엽니다.
 */
public class CraftingTableListener implements Listener {

    private final Rpg plugin;

    public CraftingTableListener(Rpg plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftingTableInteract(PlayerInteractEvent event) {
        // 우클릭 + 블럭 클릭 이벤트인지 확인
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        // 클릭한 블럭이 제작대인지 확인
        if (clickedBlock == null || clickedBlock.getType() != Material.CRAFTING_TABLE) {
            return;
        }

        Player player = event.getPlayer();
        
        // [중요] 바닐라 제작대 GUI가 열리는 것을 막습니다.
        event.setCancelled(true);

        // 우리의 커스텀 제작 GUI를 엽니다.
        new CustomCraftGUI(plugin).open(player);
    }
}