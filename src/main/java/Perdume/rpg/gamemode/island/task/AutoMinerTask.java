package Perdume.rpg.gamemode.island.task;

import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.IslandData;
import Perdume.rpg.gamemode.island.upgrade.IslandUpgrade;
import Perdume.rpg.gamemode.island.util.IslandProductionUtil;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 각 섬의 자동 생산을 담당하는 BukkitRunnable 클래스입니다.
 * Island 객체가 로드될 때 함께 시작되어 주기적으로 아이템을 생산합니다.
 */
public class AutoMinerTask extends BukkitRunnable {

    private final Island island;

    public AutoMinerTask(Island island) {
        this.island = island;
    }

    @Override
    public void run() {
        if (island.getWorld() == null) {
            this.cancel();
            return;
        }

        IslandData islandData = island.getData();
        Inventory storage = island.getStorage();

        if (storage.firstEmpty() == -1) {
            // (선택사항) 여기에 섬 주인에게 창고가 꽉 찼다고 알리는 로직을 추가할 수 있습니다.
            return;
        }

        // 1. 멀티드랍 계산
        int dropAmount = 1;
        double multiDropChance = IslandUpgrade.MULTI_DROP.getValue(islandData.getMultiDropTier());
        double random = ThreadLocalRandom.current().nextDouble(100.0);

        if (islandData.getMultiDropTier() == 5) { // T5는 3배 드랍 가능성이 있음
            if (random < 5.0) dropAmount = 3;
            else if (random < 35.0) dropAmount = 2; // 5% + 30%
        } else {
            if (random < multiDropChance) dropAmount = 2;
        }

        // 2. 생산할 아이템 결정 (기본 드롭 테이블)
        ItemStack itemToProduce = IslandProductionUtil.generateRandomItem(islandData);
        itemToProduce.setAmount(dropAmount);

        itemToProduce.setAmount(dropAmount);

        // 3. 아이템을 보관함에 추가
        storage.addItem(itemToProduce);
    }
}
