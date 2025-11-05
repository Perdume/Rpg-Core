package Perdume.rpg.gamemode.island.gui;

import Perdume.rpg.core.util.ItemFactory;
import Perdume.rpg.gamemode.island.Island;
import Perdume.rpg.gamemode.island.IslandData;
import Perdume.rpg.gamemode.island.upgrade.IslandUpgrade;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 섬 업그레이드 전용 GUI를 생성하고 관리하는 클래스입니다.
 */
public class IslandUpgradeGUI {

    private final Island island; // '건설 현장' Island 객체
    private final Inventory inventory;

    public IslandUpgradeGUI(Island island) {
        this.island = island;
        this.inventory = Bukkit.createInventory(null, 27, "§8[ 섬 업그레이드 ]");
        initializeItems();
    }

    private void initializeItems() {
        IslandData islandData = island.getData(); // 데이터는 getData()로 접근

        inventory.setItem(10, createUpgradeItem(IslandUpgrade.AUTO_MINER, islandData.getAutoMinerTier(), Material.DIAMOND_PICKAXE));
        inventory.setItem(12, createUpgradeItem(IslandUpgrade.RARE_DROP, islandData.getRareDropTier(), Material.DIAMOND));
        inventory.setItem(14, createUpgradeItem(IslandUpgrade.MULTI_DROP, islandData.getMultiDropTier(), Material.HOPPER));
        inventory.setItem(16, createUpgradeItem(IslandUpgrade.STORAGE, islandData.getStorageTier(), Material.CHEST));
    }

    /**
     * 업그레이드 아이콘 ItemStack을 동적으로 생성합니다.
     * @param upgrade 업그레이드 종류 (Enum)
     * @param currentTier 현재 섬의 티어
     * @param iconMaterial 아이콘으로 사용할 아이템 종류
     * @return 정보가 담긴 ItemStack
     */
    private ItemStack createUpgradeItem(IslandUpgrade upgrade, int currentTier, Material iconMaterial) {
        ItemFactory factory = new ItemFactory(iconMaterial)
                .setDisplayName("§a§l" + upgrade.getUpgradeName() + " §e(Lv." + currentTier + ")");

        List<String> lore = new ArrayList<>();
        lore.add("§7-------------------------");

        // 현재 티어의 효과 표시
        lore.add("§f현재 효과: " + formatValue(upgrade, currentTier));

        // 다음 티어의 효과 표시 (최대 레벨이 아닐 경우)
        if (currentTier < 5) {
            lore.add("§f다음 효과: " + formatValue(upgrade, currentTier + 1));
            lore.add(" ");
            lore.add("§e클릭하여 업그레이드!");
            // TODO: 업그레이드 비용 표시 로직 추가
        } else {
            lore.add(" ");
            lore.add("§c최대 레벨입니다.");
        }
        lore.add("§7-------------------------");

        factory.setLore(lore.toArray(new String[0]));
        return factory.build();
    }

    /**
     * 업그레이드 종류에 따라 값의 형식을 예쁘게 변환합니다. (예: 초, %, 줄)
     */
    private String formatValue(IslandUpgrade upgrade, int tier) {
        double value = upgrade.getValue(tier);
        switch (upgrade) {
            case AUTO_MINER:
                return "§e" + value + "초§f마다 생산";
            case RARE_DROP:
                return "§b희귀 광물§f 드랍률 §e+" + value + "%p";
            case MULTI_DROP:
                return "§6멀티드랍§f 확률 §e" + value + "%";
            case STORAGE:
                return "§d보관함§f 크기 §e" + (int)value + "줄";
            default:
                return String.valueOf(value);
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}