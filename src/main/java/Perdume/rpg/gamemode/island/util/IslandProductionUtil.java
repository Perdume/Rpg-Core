package Perdume.rpg.gamemode.island.util;

import Perdume.rpg.core.item.twisted_item;
import Perdume.rpg.gamemode.island.IslandData;
import Perdume.rpg.gamemode.island.upgrade.IslandUpgrade;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.ThreadLocalRandom;

/**
 * [리팩토링됨] 섬의 자동 생산 아이템을 생성하는 모든 로직을 중앙에서 관리하는 유틸리티 클래스입니다.
 * 일반 광물 드랍 로직을 가중치 기반 테이블로 개선하여 확장성을 높였습니다.
 */
public class IslandProductionUtil {

    /**
     * [내부 enum] 일반 광물의 드랍 테이블과 가중치를 정의합니다.
     * 이곳만 수정하면 일반 광물의 종류와 드랍률을 쉽게 변경할 수 있습니다.
     */
    private enum NormalOreDrop {
        DIAMOND(Material.DIAMOND, 2),       // 가중치 2 (약 2%)
        RAW_IRON(Material.RAW_IRON, 8),     // 가중치 8 (약 8%)
        COAL(Material.COAL, 20),          // 가중치 20 (약 20%)
        COBBLESTONE(Material.COBBLESTONE, 70); // 가중치 70 (약 70%)

        private final Material material;
        private final double weight;

        NormalOreDrop(Material material, double weight) {
            this.material = material;
            this.weight = weight;
        }

        public Material getMaterial() { return material; }
        public double getWeight() { return weight; }
    }

    // 모든 일반 광물의 가중치 합계를 미리 계산해둡니다.
    private static final double TOTAL_NORMAL_ORE_WEIGHT;
    static {
        double total = 0;
        for (NormalOreDrop drop : NormalOreDrop.values()) {
            total += drop.getWeight();
        }
        TOTAL_NORMAL_ORE_WEIGHT = total;
    }


    /**
     * 섬의 업그레이드 상태에 따라 단일 아이템을 생산합니다.
     * @param data 섬의 데이터
     * @return 생성된 아이템스택
     */
    public static ItemStack generateRandomItem(IslandData data) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 1. 희귀 드랍 (뒤틀린 광물)이 나올지 먼저 시도합니다.
        double rareDropBonus = IslandUpgrade.RARE_DROP.getValue(data.getRareDropTier());
        for (twisted_item ore : twisted_item.values()) {
            double chance = ore.getDropChance() + rareDropBonus;
            if (random.nextDouble(100.0) < chance) {
                return ore.getItemStack(1); // 뒤틀린 광물 당첨!
            }
        }

        // 2. [개선됨] 뒤틀린 광물이 나오지 않았다면, 가중치 기반으로 일반 광물을 생산합니다.
        double randomWeight = random.nextDouble() * TOTAL_NORMAL_ORE_WEIGHT; // 0부터 전체 가중치 합계 사이의 난수 생성

        for (NormalOreDrop drop : NormalOreDrop.values()) {
            randomWeight -= drop.getWeight();
            if (randomWeight <= 0) {
                return new ItemStack(drop.getMaterial()); // 당첨!
            }
        }

        // 만약을 대비한 기본값 (도달할 확률 거의 없음)
        return new ItemStack(Material.COBBLESTONE);
    }
}

