package Perdume.rpg.system;

import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.Random;

public class PortalParticleTask extends BukkitRunnable {

    private final Rpg plugin;
    private final PortalManager portalManager;
    private final Random random = new Random();

    public PortalParticleTask(Rpg plugin) {
        this.plugin = plugin;
        this.portalManager = plugin.getPortalManager();
    }

    @Override
    public void run() {
        // [핵심] 현재 서버에 로드된 모든 월드를 순회합니다.
        for (World world : Bukkit.getWorlds()) {
            // 해당 월드에 있는 플레이어들에게만 파티클을 보여주기 위함 (최적화)
            if (world.getPlayers().isEmpty()) {
                continue;
            }

            // 이 월드에 해당하는 포탈들을 찾습니다.
            portalManager.getPortalsInWorld(world).forEach(portal -> {
                BoundingBox region = portal.region();
                
                // 포탈 영역 내부에 10개의 파티클을 무작위로 소환합니다.
                for (int i = 0; i < 10; i++) {
                    double x = region.getMinX() + (region.getMaxX() - region.getMinX()) * random.nextDouble();
                    double y = region.getMinY() + (region.getMaxY() - region.getMinY()) * random.nextDouble();
                    double z = region.getMinZ() + (region.getMaxZ() - region.getMinZ()) * random.nextDouble();
                    
                    world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, x, y, z, 1, 0, 0, 0, 0);
                }
            });
        }
    }
}