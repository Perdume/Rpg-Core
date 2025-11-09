package Perdume.rpg.core.item.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.item.crafting.gui.CustomCraftGUI;
import Perdume.rpg.core.item.gui.EnhancementGUI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.EnumSet;

/**
 * [신규] 제작대, 모루, 마법부여대 등
 * 기능성 블럭 우클릭을 감지하여 커스텀 GUI를 엽니다.
 * (기존 CraftingTableListener를 대체합니다)
 */
public class BlockInteractListener implements Listener {

    private final Rpg plugin;
    
    // 모루 재질 세트
    private static final EnumSet<Material> ANVILS = EnumSet.of(
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL
    );

    public BlockInteractListener(Rpg plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent event) {
        // 우클릭 + 블럭 클릭 이벤트인지 확인
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();
        Material type = clickedBlock.getType();

        // 1. 제작대 (CraftingTableListener.java의 로직)
        if (type == Material.CRAFTING_TABLE) {
            event.setCancelled(true);
            new CustomCraftGUI(plugin).open(player);
            return;
        }
        
        // 2. 모루 (신규 강화 GUI)
        if (ANVILS.contains(type)) {
            event.setCancelled(true);
            new EnhancementGUI().open(player);
            return;
        }

        // 3. 마법부여대 (신규 인챈트 GUI)
        if (type == Material.ENCHANTING_TABLE) {
            event.setCancelled(true);
            // new PotentialGUI().open(player); // 4단계에서 구현
            player.sendMessage("§e[WIP] 커스텀 인챈트(큐브) 시스템은 현재 준비 중입니다.");
            return;
        }
    }
}