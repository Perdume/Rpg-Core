package Perdume.rpg.gamemode.field;

import Perdume.rpg.Rpg;
import Perdume.rpg.gamemode.field.mob.FieldMobFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class HuntingGroundInstance {

    private final Rpg plugin;
    private final World world;
    private final List<Location> spawnPoints = new ArrayList<>();
    private final List<UUID> spawnedMobs = new ArrayList<>();
    private final Random random = new Random();
    private BukkitTask spawnerTask;
    private String mobId;
    private int maxMobs;
    private int respawnTime;

    public HuntingGroundInstance(Rpg plugin, World world, String templateName) {
        this.plugin = plugin;
        this.world = world;
        loadSettings(templateName);
        startSpawning(); // 생성과 동시에 스포너 활성화
    }

    private void loadSettings(String templateName) {
        File configFile = new File(plugin.getDataFolder(), "worlds/field/" + templateName + ".yml");
        if (!configFile.exists()) {
            Rpg.log.warning("사냥터(" + world.getName() + ")의 설정 파일(" + templateName + ".yml)을 찾을 수 없어, 기본값으로 작동합니다.");
            this.mobId = "FieldSlime"; // 예시 기본값
            this.maxMobs = 10;
            this.respawnTime = 100; // 5초
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        List<?> rawList = config.getList("spawn-points");
        if (rawList != null) {
            rawList.stream()
                    .filter(obj -> obj instanceof Vector) // Vector 타입으로 저장됨
                    .map(obj -> ((Vector) obj).toLocation(this.world)) // 현재 월드 기준으로 Location 객체 생성
                    .forEach(this.spawnPoints::add);
        }

        this.mobId = config.getString("mob-id", "FieldSlime");
        this.maxMobs = config.getInt("max-mobs", 20);
        this.respawnTime = config.getInt("respawn-time", 5) * 20; // 설정 파일에는 초 단위, 코드에서는 틱으로 변환
    }

    /**
     * [핵심] 이 인스턴스 전용 몬스터 스폰 타이머를 시작합니다.
     */
    private void startSpawning() {
        if (spawnPoints.isEmpty()) {
            Rpg.log.warning("사냥터(" + world.getName() + ")에 스폰 지점이 설정되지 않아, 몬스터가 스폰되지 않습니다.");
            return;
        }

        this.spawnerTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 인스턴스가 종료되었거나, 플레이어가 없으면 타이머 중지
                if (isFinished()) {
                    this.cancel();
                    return;
                }

                // 죽은 몹 정리
                spawnedMobs.removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());
                // 최대 개체 수까지 몹 소환
                while (spawnedMobs.size() < maxMobs) {
                    // 스폰 포인트 목록에서 무작위 위치를 선택
                    Location spawnLoc = spawnPoints.get(random.nextInt(spawnPoints.size()));
                    FieldMobFactory.spawnMob(mobId, spawnLoc)
                            .ifPresent(activeMob -> spawnedMobs.add(activeMob.getUniqueId()));
                }
            }
        }.runTaskTimer(plugin, this.respawnTime, this.respawnTime); // 설정된 리스폰 시간 간격으로 반복
    }

    public boolean isFinished() {
        return world.getPlayers().isEmpty();
    }

    public void cleanup() {
        if (spawnerTask != null && !spawnerTask.isCancelled()) {
            spawnerTask.cancel();
        }
        spawnedMobs.forEach(uuid -> {
            if (Bukkit.getEntity(uuid) != null) Bukkit.getEntity(uuid).remove();
        });
        spawnedMobs.clear();
    }

    public World getWorld() {
        return world;
    }
}