package Perdume.rpg.gamemode.island;

import Perdume.rpg.Rpg;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 모든 섬의 영구 데이터를 파일(.yml)로 저장하고 불러오는 클래스입니다.
 */
public class IslandDataManager {

    private final Rpg plugin;
    private final File dataFolder;

    public IslandDataManager(Rpg plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "island_data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void saveIsland(Island island) {
        if (island == null) return;
        File islandFile = new File(dataFolder, island.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("owner", island.getOwner().toString());
        config.set("members", island.getMembers().stream().map(UUID::toString).collect(Collectors.toList()));

        // [핵심] Location 객체를 통째로 저장하는 대신, 각 정보를 분리하여 저장합니다.
        if (island.getSpawnLocation() != null) {
            Location loc = island.getSpawnLocation();
            config.set("spawn-location.world", loc.getWorld().getName());
            config.set("spawn-location.x", loc.getX());
            config.set("spawn-location.y", loc.getY());
            config.set("spawn-location.z", loc.getZ());
            config.set("spawn-location.yaw", loc.getYaw());
            config.set("spawn-location.pitch", loc.getPitch());
        }

        try {
            config.save(islandFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Island loadIsland(String islandId) {
        File islandFile = new File(dataFolder, islandId + ".yml");
        if (!islandFile.exists()) return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(islandFile);

        try {
            UUID owner = UUID.fromString(config.getString("owner"));
            List<UUID> members = config.getStringList("members").stream().map(UUID::fromString).collect(Collectors.toList());

            Location spawnLocation = null;
            // [핵심] spawn-location 섹션이 있을 때만 좌표를 읽어옵니다.
            if (config.isConfigurationSection("spawn-location")) {
                // 월드 이름만 먼저 읽어옵니다. 월드 객체는 나중에 SkyblockManager가 채워 넣습니다.
                String worldName = config.getString("spawn-location.world");
                double x = config.getDouble("spawn-location.x");
                double y = config.getDouble("spawn-location.y");
                double z = config.getDouble("spawn-location.z");
                float yaw = (float) config.getDouble("spawn-location.yaw");
                float pitch = (float) config.getDouble("spawn-location.pitch");

                // 월드를 null로 둔 채 Location 객체를 생성합니다.
                spawnLocation = new Location(null, x, y, z, yaw, pitch);
            }

            return new Island(islandId, owner, members, spawnLocation);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public void deleteIslandData(String islandId) {
        File islandFile = new File(dataFolder, islandId + ".yml");
        if (islandFile.exists()) {
            islandFile.delete();
        }
    }
}