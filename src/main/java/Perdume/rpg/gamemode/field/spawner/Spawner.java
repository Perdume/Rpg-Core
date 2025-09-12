package Perdume.rpg.gamemode.field.spawner;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.field.HuntingGroundInstance;
import Perdume.rpg.gamemode.field.mob.FieldMobFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Spawner {

    private final Rpg plugin;
    private final HuntingGroundInstance instance; // 이 스포너가 속한 인스턴스
    private final String id;
    private final String mobId;
    private final Location center;
    private final int maxMobs;
    private final int radius;
    private final int respawnTime; // 틱 (20 = 1초)

    private final List<UUID> spawnedMobs = new ArrayList<>();
    private final Random random = new Random();
    private BukkitTask task;

    public Spawner(Rpg plugin, HuntingGroundInstance instance, String id, String mobId, Location center, int maxMobs, int radius, int respawnTime) {
        this.plugin = plugin;
        this.instance = instance;
        this.id = id;
        this.mobId = mobId;
        this.center = center;
        this.maxMobs = maxMobs;
        this.radius = radius;
        this.respawnTime = respawnTime;

        // 스포너 활성화
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (instance.isFinished()) { // 인스턴스가 종료되면 스포너도 멈춤
                    this.cancel();
                    return;
                }
                tick();
            }
        }.runTaskTimer(plugin, 0L, this.respawnTime);
    }

    private void tick() {
        // 죽은 몹 정리
        spawnedMobs.removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());

        // 최대치까지 몹 소환
        if (spawnedMobs.size() < maxMobs) {
            // 스폰 지점 주변 랜덤 위치 계산
            Location spawnLoc = center.clone().add(
                    (random.nextDouble() - 0.5) * radius * 2,
                    0,
                    (random.nextDouble() - 0.5) * radius * 2
            );

            // [핵심 수정] FieldMobFactory를 통해 몬스터를 소환하고, 성공 시 UUID를 리스트에 추가
            FieldMobFactory.spawnMob(mobId, spawnLoc)
                    .ifPresent(activeMob -> spawnedMobs.add(activeMob.getUniqueId()));
        }
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        // 모든 소환된 몹 제거
        spawnedMobs.forEach(uuid -> {
            if (Bukkit.getEntity(uuid) != null) {
                Bukkit.getEntity(uuid).remove();
            }
        });
        spawnedMobs.clear();
    }
}