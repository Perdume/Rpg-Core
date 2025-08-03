package Perdume.rpg.gamemode.island.listener;

import Perdume.rpg.Rpg;
import Perdume.rpg.system.SkyblockManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class IslandWorldListener implements Listener {

    private final Rpg plugin;
    private final SkyblockManager skyblockManager;

    public IslandWorldListener(Rpg plugin) {
        this.plugin = plugin;
        this.skyblockManager = plugin.getSkyblockManager();
    }

    // 플레이어가 텔레포트할 때
    @EventHandler
    public void onWorldChange(PlayerTeleportEvent event) {
        // 이전 월드와 새로운 월드가 다를 때만
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            checkAndUnloadIsland(event.getFrom().getWorld());
        }
    }

    // 플레이어가 접속 종료할 때
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        checkAndUnloadIsland(event.getPlayer().getWorld());
    }

    /**
     * 특정 월드가 비어있는지 확인하고, 비어있다면 언로드를 예약하는 메소드
     * @param world 확인할 월드
     */
    private void checkAndUnloadIsland(World world) {
        // 1. 이름이 "Island--"로 시작하는 월드인지 확인
        if (world == null || !world.getName().startsWith("Island--")) {
            return;
        }

        // 2. [핵심] 바로 언로드하지 않고, 1틱 후에 월드에 플레이어가 있는지 확인
        // (다른 플레이어가 동시에 입장하는 경우를 대비)
        new BukkitRunnable() {
            @Override
            public void run() {
                // 3. 월드에 플레이어가 한 명도 없다면
                if (world.getPlayers().isEmpty()) {
                    String islandId = world.getName().replace("Island--", "");
                    
                    // 4. SkyblockManager에게 해당 섬의 저장을 요청
                    Rpg.log.info("섬(" + world.getName() + ")에 플레이어가 없어 10초 후 언로드를 시작합니다.");
                    // 5. 10초의 유예 시간을 더 주어, 혹시 모를 재입장/방문자를 기다림
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if(world.getPlayers().isEmpty()) {
                                skyblockManager.unloadIsland(islandId);
                            }
                        }
                    }.runTaskLater(plugin, 200L); // 10초 (20틱 * 10)
                }
            }
        }.runTaskLater(plugin, 1L); // 1틱
    }
}