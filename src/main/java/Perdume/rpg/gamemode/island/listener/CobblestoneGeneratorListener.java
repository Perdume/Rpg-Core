package Perdume.rpg.gamemode.island.listener;

import Perdume.rpg.Rpg;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

public class CobblestoneGeneratorListener implements Listener {

    private final Rpg plugin;
    // [핵심] 광물 생성 확률 테이블 (가중치 기반)
    private final NavigableMap<Double, Material> oreTable = new TreeMap<>();

    public CobblestoneGeneratorListener(Rpg plugin) {
        this.plugin = plugin;
        
        // --- 광물 생성 확률을 이곳에서 설정합니다 ---
        // 예: 1000점 만점 중, COAL은 100점(10%)의 가중치를 가집니다.
        oreTable.put(500.0, Material.COBBLESTONE); // 50%
        oreTable.put(500.0 + 200.0, Material.COAL_ORE);       // 20%
        oreTable.put(700.0 + 150.0, Material.IRON_ORE);       // 15%
        oreTable.put(850.0 + 100.0, Material.GOLD_ORE);       // 10%
        oreTable.put(950.0 + 45.0, Material.DIAMOND_ORE);    // 4.5%
        oreTable.put(995.0 + 5.0, Material.EMERALD_ORE);      // 0.5%
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        
        // 1. 생성된 블록이 우리 '섬 월드' 안에 있는지 확인합니다.
        if (block.getWorld().getName().startsWith("Island--")) {
            // 2. 생성된 블록이 '코블스톤'인지 확인합니다.
            if (event.getNewState().getType() == Material.COBBLESTONE) {
                // TODO: 용암과 물에 의해 생성되었는지 더 정확하게 확인하는 로직 추가 가능
                
                // 3. 확률 테이블에서 무작위로 광물을 선택합니다.
                double random = ThreadLocalRandom.current().nextDouble() * 1000; // 0 ~ 999.99 사이의 난수
                Material randomOre = oreTable.higherEntry(random).getValue();
                
                // 4. [핵심] 원래 생성되려던 코블스톤 대신, 우리가 선택한 광물로 교체합니다.
                // 다음 틱(tick)에 블록을 변경하여, 다른 플러그인과의 충돌을 방지하고 안정성을 높입니다.
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    block.setType(randomOre);
                });
            }
        }
    }
}