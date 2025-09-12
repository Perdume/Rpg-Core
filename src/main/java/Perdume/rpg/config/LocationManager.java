package Perdume.rpg.config; // 사용하시는 패키지 경로

import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * locations.yml 파일을 읽고, 쓰고, 관리하는 모든 로직을 책임지는 클래스입니다.
 */
public class LocationManager {

    private static File locationsFile;
    private static FileConfiguration locationsConfig;
    // 로드된 위치 정보를 메모리에 저장해두어, 필요할 때마다 빠르게 사용
    private static final Map<String, Location> spawnLocations = new HashMap<>();

    /**
     * Rpg 플러그인 활성화 시 호출되어, locations.yml 파일을 준비하고 로드합니다.
     */
    public static void initialize(Rpg plugin) {
        locationsFile = new File(plugin.getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            // jar 파일 내에 있는 기본 locations.yml 파일을 plugins/Rpg/ 폴더로 복사
            plugin.saveResource("locations.yml", false);
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
        loadLocationsFromConfig();
    }

    /**
     * locations.yml 파일의 'spawn-locations' 섹션을 읽어와 메모리에 저장합니다.
     */
    private static void loadLocationsFromConfig() {
        spawnLocations.clear();
        ConfigurationSection section = locationsConfig.getConfigurationSection("spawn-locations");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                // [핵심] world, x, y, z, yaw, pitch를 각각의 키로 읽어옵니다.
                String worldName = section.getString(key + ".world");
                if (worldName == null) {
                    Rpg.log.warning("'" + key + "' 스폰 위치 로드 실패: 'world' 정보가 없습니다.");
                    continue;
                }
                
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    Rpg.log.warning("'" + key + "' 스폰 위치 로드 실패: 월드 '" + worldName + "'을(를) 찾을 수 없습니다.");
                    continue;
                }
                
                double x = section.getDouble(key + ".x");
                double y = section.getDouble(key + ".y");
                double z = section.getDouble(key + ".z");
                float yaw = (float) section.getDouble(key + ".yaw");
                float pitch = (float) section.getDouble(key + ".pitch");

                spawnLocations.put(key, new Location(world, x, y, z, yaw, pitch));
            }
            Rpg.log.info(spawnLocations.size() + "개의 스폰 위치를 성공적으로 불러왔습니다.");
        } else {
             Rpg.log.warning("'locations.yml' 파일에 'spawn-locations:' 항목이 없습니다. 관리자가 스폰 지점을 설정해야 합니다.");
        }
    }

    /**
     * 새로운 스폰 지점을 설정할 때도, 각 좌표를 나누어 저장합니다.
     */
    public static void setSpawnLocation(String id, Location loc) {
        String path = "spawn-locations." + id;
        locationsConfig.set(path + ".world", loc.getWorld().getName());
        locationsConfig.set(path + ".x", loc.getX());
        locationsConfig.set(path + ".y", loc.getY());
        locationsConfig.set(path + ".z", loc.getZ());
        locationsConfig.set(path + ".yaw", loc.getYaw());
        locationsConfig.set(path + ".pitch", loc.getPitch());
        saveConfig();
        spawnLocations.put(id, loc); // 메모리에도 즉시 반영
    }

    public static boolean removeSpawnLocation(String id) {
        if (!locationsConfig.contains("spawn-locations." + id)) return false;
        locationsConfig.set("spawn-locations." + id, null);
        saveConfig();
        spawnLocations.remove(id);
        return true;
    }

    private static void saveConfig() {
        try {
            locationsConfig.save(locationsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // --- 유틸리티 메소드 ---
    public static FileConfiguration getLocationsConfig() {
        return locationsConfig;
    }

    public static Map<String, Location> getSpawnLocations() {
        return Collections.unmodifiableMap(spawnLocations);
    }

    public static Location getSpawnLocation(String id) {
        return spawnLocations.get(id);
    }
    
    public static void sendSpawnList(Player player) {
        player.sendMessage("§6--- 등록된 스폰 지점 목록 ---");
        if (spawnLocations.isEmpty()) {
            player.sendMessage("§c등록된 스폰 지점이 없습니다.");
            return;
        }
        spawnLocations.forEach((id, loc) -> {
            player.sendMessage(String.format("§a- %s §7(월드: %s, 좌표: %.1f, %.1f, %.1f)",
                    id, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
        });
    }
    public void reload() {
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
        loadLocationsFromConfig();
        Rpg.log.info("'locations.yml' 파일을 다시 불러왔습니다.");
    }
}