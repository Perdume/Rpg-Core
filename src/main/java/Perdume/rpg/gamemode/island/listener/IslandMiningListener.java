package Perdume.rpg.gamemode.island.listener;

import Perdume.rpg.core.item.twisted_item;
import Perdume.rpg.system.SkyblockManager;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 섬 월드에서 플레이어가 광물을 채굴할 때,
 * SpecialOre enum에 정의된 확률에 따라 특별한 아이템을 드롭하는 역할을 담당하는 리스너입니다.
 */
public class IslandMiningListener implements Listener {

    private final SkyblockManager skyblockManager;

    public IslandMiningListener(SkyblockManager skyblockManager) {
        this.skyblockManager = skyblockManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreakInIsland(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // 1. 서바이벌 모드가 아니거나, OP의 창의적인 활동이 아닐 경우 무시
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        Block block = event.getBlock();

        // 2. 현재 월드가 SkyblockManager에 의해 관리되는 '섬 월드'가 아니면 무시
        if (!skyblockManager.isIslandWorld(block.getWorld())) {
            return;
        }

        // 3. 부서진 블록에 해당하는 SpecialOre 정보가 있는지 조회 (매우 빠름)
        Optional<twisted_item> specialOreOpt = twisted_item.getBySource(block.getType());

        // 4. 해당하는 SpecialOre 정보가 있을 경우에만 확률 계산 로직 실행
        specialOreOpt.ifPresent(specialOre -> {
            // 0.0부터 100.0 사이의 난수 생성
            double chance = ThreadLocalRandom.current().nextDouble(100.0);

            // 5. 난수가 설정된 드롭 확률보다 낮으면 당첨
            if (chance < specialOre.getDropChance() * 10) {
                // enum에 정의된 메소드를 호출하여 아이템 생성
                ItemStack specialDrop = specialOre.getItemStack(1);

                // 6. 기존 광물 드롭을 취소하고, 특수 광물을 드롭
                event.setDropItems(false);
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), specialDrop);
            }
        });
    }
}