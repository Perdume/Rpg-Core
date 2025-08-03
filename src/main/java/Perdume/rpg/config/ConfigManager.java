package Perdume.rpg.config;

import Perdume.rpg.Rpg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Rpg plugin;
    private FileConfiguration locationsConfig;
    private File locationsFile;

    private final Map<String, Location> spawnLocations = new HashMap<>();

    public ConfigManager(Rpg plugin) {
        this.plugin = plugin;
    }

    public void loadLocations() {
        locationsFile = new File(plugin.getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            plugin.saveResource("locations.yml", false);
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);

        spawnLocations.clear();
        ConfigurationSection section = locationsConfig.getConfigurationSection("spawn-locations");
        if (section != null) {
            for (String key : section.getKeys(false)) {

                // [핵심] coordinates 항목이 존재하는지 먼저 안전하게 확인합니다.
                String coordsString = section.getString(key + ".coordinates");
                if (coordsString == null || coordsString.isEmpty()) {
                    Rpg.log.warning("'" + key + "' 스폰 위치 로드 실패: coordinates 정보가 없습니다.");
                    continue; // 이 스폰 지점은 건너뛰고 다음으로 넘어감
                }

                String[] coords = coordsString.split(", ");
                if (coords.length == 6) {
                    Location loc = new Location(
                            Bukkit.getWorld(coords[0]),
                            Double.parseDouble(coords[1]),
                            Double.parseDouble(coords[2]),
                            Double.parseDouble(coords[3]),
                            Float.parseFloat(coords[4]),
                            Float.parseFloat(coords[5])
                    );
                    spawnLocations.put(key, loc);
                } else {
                    Rpg.log.warning("'" + key + "' 스폰 위치 로드 실패: coordinates 형식이 잘못되었습니다.");
                }
            }
            Rpg.log.info(spawnLocations.size() + "개의 스폰 위치를 불러왔습니다.");
        }
    }

    public FileConfiguration getLocationsConfig() {
        return locationsConfig;
    }

    public Location getSpawnLocation(String id) {
        return spawnLocations.get(id);
    }
}